/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node.websocket.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.openmuc.jeebus.ship.api.DisconnectReason;
import org.openmuc.jeebus.ship.node.ShipNodeContext;
import org.openmuc.jeebus.ship.node.websocket.AuthenticatedConnection;
import org.openmuc.jeebus.ship.node.websocket.WebSocketHandler;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;

import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

public class ShipServerHandler extends WebSocketHandler implements
    AuthenticatedConnection {

    private final ShipServer server;

    public ShipServerHandler(ShipNodeContext nodeContext, ShipServer server) {
        super(nodeContext, server.getSocketListener());
        this.server = server;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        InetSocketAddress remoteAddr = (InetSocketAddress) ctx
            .channel()
            .remoteAddress();
        int remotePort = remoteAddr.getPort();
        log.info(
            "{} accepted a connection with client (port {})",
            nodeContext.getLogPrefix(),
            remotePort
        );

        // replace the logPrefix in the nodeContext with the specific one, while keeping other variables the same
        String specificLogPrefix = nodeContext
            .getLogPrefix()
            .replace(")", ", client port " + remotePort + ")");
        nodeContext = new ShipNodeContext(
            nodeContext.getConfig(),
            specificLogPrefix,
            nodeContext.getConnHandler(),
            nodeContext.getOwnShipId()
        );

        this.channel = ctx.channel();

        server.addHandler(this);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        server.removeHandler(this);
        log.info("{}: connection was closed", nodeContext.getLogPrefix());
    }

    @Override
    public synchronized void channelRead0(
        ChannelHandlerContext ctx,
        Object msg
    ) {
        if (msg instanceof FullHttpMessage) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        }
        else if (msg instanceof WebSocketFrame) {
            WebSocketFrame frame = (WebSocketFrame) msg;
            if (isPingPongFrame(frame)) {
                handlePingPongFrame(frame);
            }
            else {
                byte[] bytes = handleWebSocketFrame(frame);

                if (bytes == null) {
                    log.warn(
                        "Received empty Message from {}. Closing SHIP connection.",
                        nodeContext.getLogPrefix()
                    );
                    if (nodeContext.getConnHandler() != null) {
                        nodeContext
                            .getConnHandler()
                            .onDisconnect(
                                DisconnectReason.ERROR,
                                connection.getApiShipConn()
                            );
                    }
                    close();
                }
                else {
                    messageBuffer.writeBytes(bytes);

                    if (frame.isFinalFragment()) {
                        bytes = messageBuffer.toByteArray();
                        messageBuffer.reset();

                        if (connection != null) {
                            connection.onMessage(bytes);
                        }
                        else {
                            log.warn(
                                "{}: no connection object to process message",
                                nodeContext.getLogPrefix()
                            );
                        }
                    }
                }
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // Handle a bad request.
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req,
                new DefaultFullHttpResponse(
                    req.protocolVersion(),
                    BAD_REQUEST,
                    ctx.alloc().buffer(0)
                )
            );
            return;
        }

        // Handshake
        WebSocketServerHandshakerFactory wsFactory
            = new WebSocketServerHandshakerFactory(getWebSocketLocation(req),
            "ship", true, 5 * 1024 * 1024
        );
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }
        else {
            handshaker.handshake(ctx.channel(), req).awaitUninterruptibly();

            if (node.isDoubleConnection(getPeerSki())) {
                doubleConnProcedure(this.getPeerSki());
            }
            connection = new ShipConnectionImpl(true, getTrustLevel(), nodeContext,
                this
            );
            shipConnRdyLatch.countDown();
        }
    }

    private void sendHttpResponse(
        ChannelHandlerContext ctx,
        FullHttpRequest req,
        FullHttpResponse res
    ) {
        HttpResponseStatus responseStatus = res.status();
        if (responseStatus.code() != 200) {
            log.warn(
                "{} received HTTP response with code: {}",
                nodeContext.getLogPrefix(),
                responseStatus.code()
            );
        }
        // Send the response and close the connection if necessary.
        boolean keepAlive = HttpUtil.isKeepAlive(req)
            && responseStatus.code() == 200;
        HttpUtil.setKeepAlive(res, keepAlive);
        ChannelFuture future = ctx.write(res); // Flushed in channelReadComplete()
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private String getWebSocketLocation(FullHttpRequest req) {
        return "wss://"
            + req.headers().get(HttpHeaderNames.HOST)
            + server.getWssPath();
    }

    public void setConnection(ShipConnectionImpl connection) {
        this.connection = connection;
    }

    public Channel getChannel() {
        return this.channel;
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.stopStateTimeouts();
        }
        channel.close().awaitUninterruptibly();
        server.removeHandler(this);
    }

    public ShipConnectionImpl getConnection() {
        return connection;
    }
}
