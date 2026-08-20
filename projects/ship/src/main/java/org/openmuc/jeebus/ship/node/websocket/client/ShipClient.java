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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

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

    public static final int TIMEOUT_MILLIS = 2 * 60 * 1000;

    public ShipClient(
        SslContext sslContext,
        InetSocketAddress socket,
        String path,
        ShipNodeContext nodeContext,
        ShipNodeImpl shipNode
    ) throws InterruptedException, URISyntaxException {
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

        start();
    }

    private void start() throws InterruptedException {
        log.info("starting client to connect to {}", uri);

        // configure client
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

        ChannelFuture future = bootstrap.connect(socket);
        if (!future.await(TIMEOUT_MILLIS)) {
            log.error("Could not open a channel to {} within {} milliseconds.",
                uri,
                TIMEOUT_MILLIS
            );
        }
        else if (!future.isSuccess()) {
            log.error("There was an error connecting to {}",
                uri,
                future.cause()
            );
        }
        future.sync();
        handler.handshakeFuture().sync();
    }

    public synchronized void stop() {
        log.info("stopping {}", nodeContext.getLogPrefix());
        if (handler.getConnection() != null) {
            handler.getConnection().stopStateTimeouts();
        }
        group.shutdownGracefully();
        shipNode.removeClient(this);
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

    public void setHandler(ShipClientHandler handler) {
        this.handler = handler;
    }

    public void setConnHandler(ConnectionHandler connHandler) {
        nodeContext.setConnHandler(connHandler);
    }

}
