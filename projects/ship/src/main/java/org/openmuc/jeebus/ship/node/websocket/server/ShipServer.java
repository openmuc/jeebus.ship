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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.node.ShipNodeContext;
import org.openmuc.jeebus.ship.node.ShipNodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShipServer {
    protected static final Logger log = LoggerFactory.getLogger(ShipServer.class);
    private final SslContext sslContext;
    private final int port;
    private final String wssPath;
    private final boolean keepAlive;
    private final String logPrefix;
    private final List<ShipServerHandler> handlers
        = Collections.synchronizedList(new ArrayList<>());

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture bootstrapFuture;

    private final ShipNodeContext nodeContext;
    private final ShipNodeImpl shipNode;

    public ShipServer(
        SslContext sslContext,
        int port,
        String wssPath,
        ShipNodeContext nodeContext,
        ShipNodeImpl shipNode,
        boolean keepAlive
    ) throws InterruptedException {
        this.sslContext = sslContext;

        this.wssPath = wssPath;

        this.keepAlive = keepAlive;

        this.nodeContext = nodeContext;
        this.shipNode = shipNode;

        this.port = port;
        this.nodeContext.setLogPrefix("SHIP server (port " + this.port + ")");
        // the logPrefix will be changed in the serverHandler later so the initial
        // prefix needs to be saved for logging
        logPrefix = this.nodeContext.getLogPrefix();

        this.start();
    }

    private synchronized void start() throws InterruptedException {
        log.info("{} started", logPrefix);

        IoHandlerFactory ioHandlerFactory = NioIoHandler.newFactory();
        bossGroup = new MultiThreadIoEventLoopGroup(1, ioHandlerFactory);
        workerGroup = new MultiThreadIoEventLoopGroup(ioHandlerFactory);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ShipServerInitializer(sslContext, nodeContext, this))
            .childOption(ChannelOption.SO_KEEPALIVE, keepAlive);

        bootstrapFuture = bootstrap.bind(port).sync();
    }

    public synchronized void stop() {
        log.info("stopping {}", logPrefix);
        handlers.forEach((handler -> {
            if (handler.getConnection() != null) {
                handler.getConnection().stopStateTimeouts();
            }
            // use handler.close() if not working, so it waits until the channel
            // closes
            handler.getChannel().close();
        }));
        try {
            bootstrapFuture.channel().close().sync();

            // syncing graceful netty shutdowns apparently creates deadlocks
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            bootstrapFuture.channel().closeFuture().sync();
            shipNode.removeServer(this);
        }
        catch (InterruptedException e) {
            log.error("exception while stopping server: ", e);
            Thread.currentThread().interrupt();
        }
    }

    public List<ShipServerHandler> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<ShipServerHandler> handlers) {
        synchronized (this.handlers) {
            this.handlers.clear();
            this.handlers.addAll(handlers);
        }
    }

    public int getPort() {
        return port;
    }

    public void setConnHandler(ConnectionHandler connHandler) {
        nodeContext.setConnHandler(connHandler);
    }

    public synchronized void addHandler(ShipServerHandler handler) {
        handlers.add(handler);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.execute(() -> {
                    if (shipNode.getClientConnectedListener() != null) {
                try {
                    if (!handler.isShipConnRdy(5)) {
                        throw new IllegalStateException(
                            "ShipConnection not ready in time");
                    }
                }
                catch (InterruptedException e) {
                    log.error(e.getMessage());
                    Thread.currentThread().interrupt();
                }
                        shipNode
                    .getClientConnectedListener()
                            .onClientConnected(handler.getShipConnection());
            }
            else {
                log.warn("{}: ClientConnectedCallBack not set", logPrefix);
            }
        });
        exec.shutdown();
    }

    public synchronized void removeHandler(ShipServerHandler handler) {
        handlers.remove(handler);
    }

    public String getWssPath() {
        return wssPath;
    }

    public ShipNodeImpl getSocketCB() {
        return shipNode;
    }
}
