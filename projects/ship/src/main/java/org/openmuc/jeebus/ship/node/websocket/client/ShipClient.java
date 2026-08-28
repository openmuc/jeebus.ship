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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.node.ShipNodeContext;
import org.openmuc.jeebus.ship.node.ShipNodeImpl;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openmuc.jeebus.ship.node.ShipNodeParameters.WSS_HANDSHAKE_TIMEOUT;
import static org.openmuc.jeebus.ship.util.ShipUtilities.toCompletableFuture;

public class ShipClient {
    private static final Logger log = LoggerFactory.getLogger(ShipClient.class);

    private final SslContext sslContext;

    public static final String WSS_PREFIX = "wss";
    private final InetSocketAddress socket;
    private final URI uri;

    private final ShipNodeContext nodeContext;

    private final ShipNodeImpl shipNode;

    private ShipClientHandler handler;

    private EventLoopGroup group;

    public static final int BOOTSTRAP_TIMEOUT = 2 * 60;

    public ShipClient(
        SslContext sslContext,
        InetSocketAddress socket,
        String path,
        ShipNodeContext nodeContext,
        ShipNodeImpl shipNode
    ) throws URISyntaxException {
        this.sslContext = sslContext;
        this.socket = socket;

        this.uri = new URI(
            WSS_PREFIX,
            null,
            socket.getAddress().getHostAddress(),
            socket.getPort(),
            fixPath(path),
            null,
            null
        );

        this.nodeContext = nodeContext;
        this.shipNode = shipNode;
    }

    public CompletableFuture<ShipClientHandler> start() {
        log.info("starting client to connect to {}", uri);

        group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());

        handler = new ShipClientHandler(
            WebSocketClientHandshakerFactory.newHandshaker(
                uri,
                WebSocketVersion.V13,
                "ship",
                false,
                new DefaultHttpHeaders()
            ),
            nodeContext,
            shipNode,
            this::stop
        );

        Bootstrap bootstrap = new Bootstrap();
        bootstrap
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel channel) {
                    ChannelPipeline pipeline = channel.pipeline();
                    SslHandler sslHandler = sslContext.newHandler(
                        channel.alloc(),
                        socket.getHostString(),
                        socket.getPort()
                    );
                    pipeline.addLast(sslHandler);

                    pipeline.addLast(
                        new HttpClientCodec(),
                        new HttpObjectAggregator(1024),
                        handler
                    );
                }
            });

        return toCompletableFuture(bootstrap.connect(socket))
            .orTimeout(BOOTSTRAP_TIMEOUT, SECONDS)
            .thenCompose(ignored -> toCompletableFuture(handler.handshakeFuture()))
            .thenApply(alsoIgnored -> handler)
            .thenCombine(
                handler.getWssHandshakeFuture()
                    .orTimeout(WSS_HANDSHAKE_TIMEOUT, SECONDS),
                (handler, ignored) -> handler
            );
    }

    public synchronized void stop() {
        if (shipNode.removeClient(this)) {
            log.info("stopping {}", nodeContext.getLogPrefix());
        }

        ShipConnectionImpl connection = handler.getConnection();

        if (connection != null) {
            connection.stopStateTimeouts();
            if (!connection.getConnectionFuture().isDone()) {
                connection
                    .getConnectionFuture()
                    .completeExceptionally(new CancellationException(
                        "SHIP client was stopped before connection could be established"
                    ));
            }
        }
        group.shutdownGracefully();
    }

    @Nonnull
    private String fixPath(String what) {
        String result = what;
        if (!result.startsWith("/")) {
            result = "/" + result;
        }
        if (!result.endsWith("/")) {
            result += "/";
        }
        return result;
    }

    public ShipClientHandler getHandler() {
        return handler;
    }

    public void setConnHandler(ConnectionHandler connHandler) {
        nodeContext.setConnHandler(connHandler);
    }

}
