/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslHandler;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.openmuc.jeebus.ship.api.DisconnectReason;
import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.node.StaticConfiguration;
import org.openmuc.jeebus.ship.node.KeyManagement;
import org.openmuc.jeebus.ship.node.ShipNodeContext;
import org.openmuc.jeebus.ship.node.ShipNodeImpl;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.websocketx.WebSocketCloseStatus.INVALID_MESSAGE_TYPE;
import static io.netty.handler.codec.http.websocketx.WebSocketCloseStatus.PROTOCOL_ERROR;
import static org.openmuc.jeebus.ship.message.MessageUtility.wrapInBinaryFrame;

public abstract class WebSocketHandler extends SimpleChannelInboundHandler<Object> implements
    AuthenticatedConnection {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ShipNodeImpl node;
    protected ShipNodeContext nodeContext;
    protected ShipConnectionImpl connection;

    // cache these values so we don't have to recompute them all the time
    private volatile transient String remoteAddress = null;
    private volatile transient String peerSki = null;

    protected Channel channel;

    protected CountDownLatch shipConnRdyLatch = new CountDownLatch(1);

    protected boolean pongReceived;

    protected final ByteArrayOutputStream messageBuffer
        = new ByteArrayOutputStream();

    protected WebSocketHandler(ShipNodeContext nodeContext, ShipNodeImpl node) {
        super(false);
        this.nodeContext = nodeContext;
        this.node = node;
    }

    public void sendMsg(byte[] msg) {
        if (channel.isActive()) {
            channel.writeAndFlush(wrapInBinaryFrame(msg));
        }
        else {
            log.error(
                "{}: last message could not be sent as the connection is closing or already closed.\nThe message was: {}",
                nodeContext.getLogPrefix(),
                MessageUtility.parseShipMsgToString(msg)
            );
        }
    }

    protected boolean isPingPongFrame(WebSocketFrame frame) {
        return frame instanceof PingWebSocketFrame
            || frame instanceof PongWebSocketFrame;
    }

    protected void handlePingPongFrame(WebSocketFrame frame) {
        if (frame instanceof PingWebSocketFrame) {
            log.debug(
                "{} received a ping frame and will respond with a pong frame",
                nodeContext.getLogPrefix()
            );
            channel.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        }
        else if (frame instanceof PongWebSocketFrame) {
            log.debug("{} received a pong frame", nodeContext.getLogPrefix());
            pongReceived = true;
        }
    }

    protected byte[] handleWebSocketFrame(WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            log.warn(
                "{} received a text frame and will terminate the connection with status code 1003",
                nodeContext.getLogPrefix()
            );
            // terminate with status code 1003 (unacceptable data)
            channel
                .writeAndFlush(new CloseWebSocketFrame(INVALID_MESSAGE_TYPE))
                .addListener(ChannelFutureListener.CLOSE);
            // listener is added to make sure channel will only close if message was written
        }
        else if (frame instanceof BinaryWebSocketFrame
            || frame instanceof ContinuationWebSocketFrame
        ) {
            ByteBuf frameContent = frame.content();

            // parse the message for logging before passing it to the state handler
            byte[] bytes = new byte[frameContent.readableBytes()];
            frameContent.readBytes(bytes);

            /* Squelch a warning about potential memory leaks when ByteBuf
             * is garbage collected before it is released.
             * See https://netty.io/wiki/reference-counted-objects.html */
            frameContent.release();

            return bytes;
        }
        else if (frame instanceof CloseWebSocketFrame) {
            // close the connection after sending
            log.info("{} received a closing frame", nodeContext.getLogPrefix());
        }
        else {
            log.warn(
                "{} unsupported frame type: {}."
                    + " Terminating connection with status code 1002",
                nodeContext.getLogPrefix(),
                frame.getClass().getName()
            );
            channel
                .writeAndFlush(new CloseWebSocketFrame(PROTOCOL_ERROR))
                .addListener(ChannelFutureListener.CLOSE);
        }

        return null;
    }

    public int getTrustLevel() {
        int trustLevel = StaticConfiguration.getTrustLevel(node.consumeAutoAccept());
        String peerSki = getPeerSki();
        if (KeyManagement.getTrustedSkis().containsKey(peerSki)) {
            trustLevel = KeyManagement.getTrustedSkis().get(peerSki).getTrustLevel();
        }

        log.info("Trust level of device with SKI {} is {}.", peerSki, trustLevel);

        return trustLevel;
    }

    @Override
    public String getPeerSki() {
        String peerSki = this.peerSki;
        if (peerSki == null) {
            SslHandler sslHandler = (SslHandler) channel.pipeline().get("SslHandler#0");
            X509Certificate certificate = null;
            try {
                Certificate[] peerCerts = sslHandler
                    .engine()
                    .getSession()
                    .getPeerCertificates();
                certificate = (X509Certificate) peerCerts[0];
            }
            catch (SSLPeerUnverifiedException e) {
                log.error("Exception while getting peer certificate");
            }
            SubjectKeyIdentifier ski = null;
            if (certificate != null) {
                ski = KeyManagement.generateSki(certificate.getPublicKey());
            }
            return this.peerSki = KeyManagement.encodeSkiAsString(ski);
        } else {
            return peerSki;
        }
    }

    public InetSocketAddress getRemoteSocketAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public String getRemoteAddress() {
        String remoteAddress = this.remoteAddress;
        if (remoteAddress == null) {
            InetSocketAddress remoteAddr = (InetSocketAddress) channel.remoteAddress();
            this.remoteAddress = remoteAddress = String.format(
                "%s:%d",
                remoteAddr.getAddress().getHostAddress(),
                remoteAddr.getPort()
            );
        }
        return remoteAddress;
    }

    protected synchronized void doubleConnProcedure(String peerSki) {
        if (node.isDoubleConnection(peerSki)) {
            log.warn("{}: double connection detected", nodeContext.getLogPrefix());
            String ownSki = node.getOwnSki();
            if (0 > ownSki.compareTo(peerSki)) {
                // own ski is bigger
                node.closeDoubleConns(this);
                if (nodeContext.getConnHandler() != null && connection != null) {
                    nodeContext
                        .getConnHandler()
                        .onDisconnect(
                            DisconnectReason.DUPLICATE_CONN,
                            connection.getApiShipConn()
                        );
                }
            }
            // TODO implement ping/pong frame when detecting double connection
            /*else {
                ScheduledExecutorService executors = Executors.newScheduledThreadPool(2);
                executors.schedule(() -> {
                    if (ch.isActive()) {
                        ch.writeAndFlush(new PingWebSocketFrame());
                        pongReceived = false;

                        executors.schedule(() -> {
                            if (ch.isActive() && !pongReceived) {
                                if (nodeCtx.getConnHandler() != null)
                                    nodeCtx.getConnHandler()
                                            .onDisconnect(DisconnectReason.ERROR, connection.getApiShipConn());
                                close();
                            }
                        }, nodeCtx.getConfig().getPongReceiveTimeout(), TimeUnit.SECONDS);
                    }
                }, 3, TimeUnit.SECONDS);
                executors.shutdown();
            }*/
        }
    }

    public boolean isShipConnRdy(int timeoutSeconds) throws InterruptedException {
        return shipConnRdyLatch.await(timeoutSeconds, TimeUnit.SECONDS);
    }

    public ShipConnectionImpl getShipConnection() {
        return this.connection;
    }

    public abstract void close();

    @Override
    public void exceptionCaught(
        ChannelHandlerContext ctx,
        Throwable cause
    ) throws Exception {
        super.exceptionCaught(ctx, cause);

        log.error("{} encountered exception: {}", nodeContext.getLogPrefix(), cause);
    }
}
