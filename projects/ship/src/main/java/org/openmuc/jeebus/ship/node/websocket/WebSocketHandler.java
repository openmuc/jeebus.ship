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
import org.bouncycastle.util.encoders.Hex;
import org.openmuc.jeebus.ship.api.ShipConnectionInterface;
import org.openmuc.jeebus.ship.api.cert.ShipAuthenticationException;
import org.openmuc.jeebus.ship.api.DisconnectReason;
import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.node.ShipNodeParameters;
import org.openmuc.jeebus.ship.node.KeyManagement;
import org.openmuc.jeebus.ship.node.ShipNodeContext;
import org.openmuc.jeebus.ship.node.ShipNodeImpl;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.*;

import static io.netty.handler.codec.http.websocketx.WebSocketCloseStatus.INVALID_MESSAGE_TYPE;
import static io.netty.handler.codec.http.websocketx.WebSocketCloseStatus.PROTOCOL_ERROR;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openmuc.jeebus.ship.message.MessageUtility.wrapInBinaryFrame;

public abstract class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ShipNodeImpl node;
    protected ShipNodeContext nodeContext;
    protected ShipConnectionImpl connection;

    // cache these values so we don't have to recompute them all the time
    private volatile transient String peerSki = null;

    protected Channel channel;

    protected final CompletableFuture<ShipConnectionInterface> wssHandshakeFuture;

    protected boolean pongReceived;

    protected final ByteArrayOutputStream messageBuffer
        = new ByteArrayOutputStream();

    protected WebSocketHandler(ShipNodeContext nodeContext, ShipNodeImpl node) {
        super(false);
        this.nodeContext = nodeContext;
        this.node = node;
        this.wssHandshakeFuture = new CompletableFuture<>();
    }

    public void sendMsg(byte[] msg) {
        if (channel.isActive()) {
            channel.writeAndFlush(wrapInBinaryFrame(msg));
        }
        // On regular closures it's OK if some messages don't make it
        else if (connection.getConnectionFuture().isCompletedExceptionally()) {
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
        int trustLevel = ShipNodeParameters.getTrustLevel(node.consumeAutoAccept());
        String peerSki = getPeerSki();
        if (node.getKeyManagement().getTrustedSkis().containsKey(peerSki)) {
            trustLevel = node
                .getKeyManagement()
                .getTrustedSkis()
                .get(peerSki)
                .getTrustLevel();
        }

        log.info("Trust level of device with SKI {} is {}.", peerSki, trustLevel);

        return trustLevel;
    }

    public String getPeerSki() {
        if (this.peerSki == null) {
            try {
                X509Certificate certificate = Arrays
                    .stream(channel
                        .pipeline()
                        .get(SslHandler.class)
                        .engine()
                        .getSession()
                        .getPeerCertificates())
                    .filter(X509Certificate.class::isInstance)
                    .findFirst()
                    .map(X509Certificate.class::cast)
                    .orElseThrow();

                this.peerSki = KeyManagement.getAuthenticatedPeerSki(
                    certificate,
                    nodeContext.getExpectedSki()
                );
            }
            catch (SSLPeerUnverifiedException |
                   NullPointerException |
                   CertificateEncodingException e
            ) {
                throw new ShipAuthenticationException(
                    "Could not generate SKI from their certificate. A secure SHIP connection is not possible.",
                    e
                );
            }
        }
        return this.peerSki;
    }

    public InetSocketAddress getRemoteSocketAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    protected boolean areWeClosingDoubleConnection(String peerSki) {
        if (!node.addCurrentRemoteSki(peerSki)) {
            log.warn("{}: double connection detected", nodeContext.getLogPrefix());

            int comparison = Arrays.compareUnsigned(
                Hex.decode(node.getOwnSki()),
                Hex.decode(peerSki)
            );

            // If we are double-connected to ourselves, let's also cancel...
            if (comparison >= 0) {
                log.warn(
                    "{}: we have the higher SKI value, so we are canceling this connection",
                    nodeContext.getLogPrefix()
                );

                /* According to SHIP:12.2.2, we SHALL only keep the most recent
                 * connection open and close all other connections to the same SHIP
                 * node. However, it makes no sense to throw away an advanced
                 * connection that may be already be used in SPINE in favor of a
                 * fresh one that may yet fail. Furthermore, as SHIP does not
                 * describe time-synchronization, there is no surefire way for client
                 * and server to reach consensus on which is the most recent
                 * connection. Lastly, as both roles are allowed to close connections
                 * in certain situations, neither side can ever be sure which
                 * connection will survive.
                 * In conclusion, immediately canceling double connections as soon as
                 * they arise seems to be the most reliable way of handling them.
                 */
                cancelFutures(new CancellationException(
                    "This is a double connection we are cancelling."));

                if (connection != null) {
                    if (nodeContext.getConnHandler() != null) {
                        nodeContext
                            .getConnHandler()
                            .onDisconnect(
                                DisconnectReason.DOUBLE_CONNECTION,
                                connection.getApiShipConnection()
                            );
                    }
                }

                this.close();
                return true;
            }
            else {
                log.warn(
                    "{}: we have the lower SKI value, so the remote should clean up",
                    nodeContext.getLogPrefix()
                );
                /* According to SHIP:12.2.2, we SHALL send a ping after 3 seconds.
                 * Everything after that is optional.
                 */
                Executors.newSingleThreadScheduledExecutor().schedule(
                    () -> channel.isActive() ? channel.writeAndFlush(new PingWebSocketFrame()) : null,
                    3,
                    SECONDS
                );
            }
        }
        return false;
    }

    protected void cancelFutures(Throwable exception) {
        if (!wssHandshakeFuture.isDone()) {
            wssHandshakeFuture.completeExceptionally(exception);
        }
        if (connection != null && !connection.getConnectionFuture().isDone()) {
            connection.getConnectionFuture().completeExceptionally(exception);
        }
    }

    public ShipConnectionImpl getShipConnection() {
        return this.connection;
    }

    public abstract void close();

    @Override
    public void exceptionCaught(
        ChannelHandlerContext ctx,
        Throwable cause
    ) {
        if (cause instanceof ShipAuthenticationException) {
            Optional
                .ofNullable(connection)
                .map(ShipConnectionImpl::getConnectionFuture)
                .ifPresent(future -> future.completeExceptionally(cause));
            this.close();
        }

        log.error(
            "{} encountered exception:",
            nodeContext.getLogPrefix(),
            cause
        );
    }

    public CompletableFuture<ShipConnectionInterface> getWssHandshakeFuture() {
        return wssHandshakeFuture;
    }

    public ShipConnectionImpl getConnection() {
        return connection;
    }
}
