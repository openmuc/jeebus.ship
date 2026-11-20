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
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.*;

import static io.netty.handler.codec.http.websocketx.WebSocketCloseStatus.ENDPOINT_UNAVAILABLE;
import static io.netty.handler.codec.http.websocketx.WebSocketCloseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.websocketx.WebSocketCloseStatus.INVALID_MESSAGE_TYPE;
import static io.netty.handler.codec.http.websocketx.WebSocketCloseStatus.NORMAL_CLOSURE;
import static io.netty.handler.codec.http.websocketx.WebSocketCloseStatus.PROTOCOL_ERROR;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openmuc.jeebus.ship.api.DisconnectReason.ERROR;
import static org.openmuc.jeebus.ship.api.DisconnectReason.REGULAR_END;
import static org.openmuc.jeebus.ship.api.DisconnectReason.REMOTE_CLOSED;
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

    private boolean closeFrameSent;
    private boolean closed;
    private boolean closedNotified;
    private boolean disconnectNotified;
    private DisconnectReason disconnectReason = ERROR;

    protected final ByteArrayOutputStream messageBuffer
        = new ByteArrayOutputStream();
    protected final Instant connectionStartDate = Instant.now();

    protected WebSocketHandler(ShipNodeContext nodeContext, ShipNodeImpl node) {
        super(false);
        this.nodeContext = nodeContext;
        this.node = node;
        this.wssHandshakeFuture = new CompletableFuture<>();
    }

    public boolean isChannelInitialized() {
        return this.channel != null;
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

        frame.release();
    }

    protected byte[] handleWebSocketFrame(WebSocketFrame frame) {
        try {
            if (frame instanceof TextWebSocketFrame) {
                log.warn(
                    "{} received a text frame and will terminate the connection with status code 1003",
                    nodeContext.getLogPrefix()
                );
                // terminate with status code 1003 (unacceptable data)
                sendCloseFrame(new CloseWebSocketFrame(INVALID_MESSAGE_TYPE));
            }
            else if (frame instanceof BinaryWebSocketFrame
                || frame instanceof ContinuationWebSocketFrame
            ) {
                ByteBuf frameContent = frame.content();

                // parse the message for logging before passing it to the state handler
                byte[] bytes = new byte[frameContent.readableBytes()];
                frameContent.readBytes(bytes);

                return bytes;
            }
            else {
                log.warn(
                    "{} unsupported frame type: {}."
                        + " Terminating connection with status code 1002",
                    nodeContext.getLogPrefix(),
                    frame.getClass().getName()
                );
                sendCloseFrame(new CloseWebSocketFrame(PROTOCOL_ERROR));
            }

            return null;
        }
        finally {
            frame.release();
        }
    }

    /**
     * Answers a close frame received from the peer as required by RFC 6455, section 5.5.1,
     * and tears the connection down.
     */
    protected void handleCloseFrame(CloseWebSocketFrame frame) {
        int statusCode = frame.statusCode();
        log.info(
            "{} received a close frame with status code {}",
            nodeContext.getLogPrefix(),
            statusCode
        );

        close(remoteCloseReason(statusCode), echoOf(statusCode));
        frame.release();
    }

    private static WebSocketCloseStatus echoOf(int statusCode) {
        /* 1005, 1006 and 1015 are reserved for local use and must never be sent, and
         * isValidStatusCode() also accepts negative values for "no status code". */
        return statusCode >= 0 && WebSocketCloseStatus.isValidStatusCode(statusCode)
            ? WebSocketCloseStatus.valueOf(statusCode)
            : NORMAL_CLOSURE;
    }

    private static DisconnectReason remoteCloseReason(int statusCode) {
        return statusCode == NORMAL_CLOSURE.code()
            || statusCode == ENDPOINT_UNAVAILABLE.code()
            ? REMOTE_CLOSED
            : ERROR;
    }

    private static DisconnectReason localCloseReason(WebSocketCloseStatus status) {
        return status.code() == NORMAL_CLOSURE.code()
            || status.code() == ENDPOINT_UNAVAILABLE.code()
            ? REGULAR_END
            : ERROR;
    }

    private static WebSocketCloseStatus toCloseStatus(DisconnectReason reason) {
        return reason == ERROR ? INTERNAL_SERVER_ERROR : NORMAL_CLOSURE;
    }

    private void sendCloseFrame(CloseWebSocketFrame frame) {
        if (closeFrameSent) {
            frame.release();
            return;
        }
        closeFrameSent = true;

        if (channel == null) {
            frame.release();
            return;
        }
        if (channel.isActive()) {
            // the listener makes sure the channel is only closed after the frame was written
            channel.writeAndFlush(frame).addListener(ChannelFutureListener.CLOSE);
        }
        else {
            frame.release();
            channel.close();
        }
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
                this.close(DisconnectReason.DOUBLE_CONNECTION);
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

    public Instant getConnectionStartDate() {
        return this.connectionStartDate;
    }

    public void close() {
        close(ERROR);
    }

    public void close(DisconnectReason reason) {
        close(reason, toCloseStatus(reason));
    }

    /**
     * Closes the connection, sending the given status code to the peer. The
     * {@link DisconnectReason} reported to the connection handler is derived from it.
     *
     * @param status
     *     the status code to send
     */
    public void close(WebSocketCloseStatus status) {
        close(localCloseReason(status), status);
    }

    private void close(DisconnectReason reason, WebSocketCloseStatus status) {
        if (closed) {
            return;
        }
        closed = true;
        disconnectReason = reason;

        if (this.peerSki != null) {
            node.removeCurrentRemoteSki(getPeerSki());
        }
        cancelFutures(new CancellationException(
            "Connection closed before it could be established"));

        messageBuffer.reset();

        if (getShipConnection() != null) {
            getShipConnection().stopStateTimeouts();
        }
        notifyConnectionHandlerOnClose();

        if (channel == null) {
            notifyClosed();
            return;
        }

        channel.closeFuture().addListener(future -> notifyClosed());

        if (!closeFrameSent) {
            sendCloseFrame(new CloseWebSocketFrame(status));
        }
    }

    /**
     * Hook for subclasses to release resources that are specific to their role.
     */
    protected void onClosed() {
    }

    private void notifyClosed() {
        if (closedNotified) {
            return;
        }
        closedNotified = true;
        onClosed();
    }

    protected boolean isClosed() {
        return closed;
    }

    private void notifyConnectionHandlerOnClose() {
        if (disconnectNotified
            || getShipConnection() == null
            || nodeContext.getConnHandler() == null
        ) {
            return;
        }
        if (getShipConnection().getConnectionFuture().isDone()
            && !getShipConnection().getConnectionFuture().isCompletedExceptionally()
        ) {
            disconnectNotified = true;
            nodeContext.getConnHandler().onDisconnect(
                disconnectReason,
                getShipConnection().getApiShipConnection()
            );
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("{}: channel is inactive", nodeContext.getLogPrefix());
        super.channelInactive(ctx);
        close();
    }

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
            "{} encountered exception",
            nodeContext.getLogPrefix(),
            cause
        );
    }

    public CompletableFuture<ShipConnectionInterface> getWssHandshakeFuture() {
        return wssHandshakeFuture;
    }

    public ShipConnectionImpl getShipConnection() {
        return connection;
    }
}
