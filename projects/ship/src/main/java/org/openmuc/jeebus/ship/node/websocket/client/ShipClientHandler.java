/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node.websocket.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import org.openmuc.jeebus.ship.api.DisconnectReason;
import org.openmuc.jeebus.ship.node.ShipNodeContext;
import org.openmuc.jeebus.ship.node.ShipNodeImpl;
import org.openmuc.jeebus.ship.node.websocket.WebSocketHandler;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;

import java.net.InetSocketAddress;

public class ShipClientHandler extends WebSocketHandler {

    private final WebSocketClientHandshaker handshaker;
    private final StopClientListener stopListener;
    private ChannelPromise handshakeFuture;

    public ShipClientHandler(
        WebSocketClientHandshaker handshaker,
        ShipNodeContext nodeContext,
        ShipNodeImpl node,
        StopClientListener stopListener
    ) {
        super(nodeContext, node);
        this.handshaker = handshaker;
        this.stopListener = stopListener;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());

        int localPort = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        log.info("client opened on port {}", localPort);
        nodeContext.setLogPrefix("SHIP client (port " + localPort + ")");

        this.channel = ctx.channel();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("{}: connection was closed", nodeContext.getLogPrefix());
        close();
    }

    @Override
    public synchronized void channelRead0(ChannelHandlerContext ctx, Object msg) {
        Channel ch = ctx.channel();

        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                log.info("{} connected", nodeContext.getLogPrefix());
                handshakeFuture.setSuccess();
            }
            catch (WebSocketHandshakeException e) {
                log.error("{} failed to connect", nodeContext.getLogPrefix());
                handshakeFuture.setFailure(e);
            }
            if (node.isDoubleConnection(getPeerSki())) {
                doubleConnProcedure(this.getPeerSki());
            }
            this.connection = new ShipConnectionImpl(
                false,
                getTrustLevel(),
                nodeContext,
                this
            );
            shipConnRdyLatch.countDown();
            return;
        }

        if (msg instanceof WebSocketFrame) {
            WebSocketFrame frame = (WebSocketFrame) msg;
            if (isPingPongFrame(frame)) {
                handlePingPongFrame(frame);
            }
            else {
                byte[] bytes = handleWebSocketFrame(frame);

                if (bytes == null) {
                    log.warn(
                        "Received empty/invalid Message or CloseWebSocketFrame"
                            + " from {}. Closing SHIP connection.",
                        nodeContext.getLogPrefix()
                    );
                    if (nodeContext.getConnHandler() != null) {
                        nodeContext
                            .getConnHandler()
                            .onDisconnect(
                                DisconnectReason.ERROR,
                                connection.getApiShipConnection()
                            );
                    }
                    stopListener.stop();
                }
                else {
                    messageBuffer.writeBytes(bytes);

                    if (frame.isFinalFragment()) {
                        bytes = messageBuffer.toByteArray();
                        messageBuffer.reset();

                        connection.onMessage(bytes);
                    }
                }
            }
        }

    }

    @Override
    public void close() {
        if(connection != null) {
            connection.stopStateTimeouts();
        }
        if (channel.isActive()) {
            channel.writeAndFlush(new CloseWebSocketFrame());
        }
        channel.close().awaitUninterruptibly();
        stopListener.stop();
    }

    public ShipConnectionImpl getConnection() {
        return connection;
    }

    public void setConnection(ShipConnectionImpl connection) {
        this.connection = connection;
    }
}
