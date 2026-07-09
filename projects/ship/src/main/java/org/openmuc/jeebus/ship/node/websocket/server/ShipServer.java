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
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.node.ShipNodeContext;
import org.openmuc.jeebus.ship.node.ShipNodeImpl;
import org.openmuc.jeebus.ship.util.ShipUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ShipServer {
    protected static final Logger log = LoggerFactory.getLogger(ShipServer.class);
    private final SslContext sslContext;
    private final String wssPath;
    private final boolean keepAlive;
    private final String logPrefix;
    private final List<ShipServerHandler> handlers
        = Collections.synchronizedList(new ArrayList<>());

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private final ShipNodeContext nodeContext;
    private final ShipNodeImpl shipNode;

    private Set<InetSocketAddress> boundSocketAddresses;
    private ChannelGroup serverChannels;

    public ShipServer(
        SslContext sslContext,
        Set<InetSocketAddress> bindAddresses,
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

        // TODO
        this.nodeContext.setLogPrefix(
            "SHIP server (" + nodeContext.getOwnShipId() + ")"
        );
        // the logPrefix will be changed in the serverHandler later so the initial
        // prefix needs to be saved for logging
        logPrefix = this.nodeContext.getLogPrefix();

        this.start(bindAddresses);
    }

    private synchronized void start(
        Set<InetSocketAddress> bindAddresses
    ) {

        log.info("starting {}", logPrefix);

        IoHandlerFactory ioHandlerFactory = NioIoHandler.newFactory();
        bossGroup = new MultiThreadIoEventLoopGroup(1, ioHandlerFactory);
        workerGroup = new MultiThreadIoEventLoopGroup(ioHandlerFactory);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ShipServerInitializer(sslContext, nodeContext, this))
            .childOption(ChannelOption.SO_KEEPALIVE, keepAlive);

        serverChannels = new DefaultChannelGroup(
            GlobalEventExecutor.INSTANCE);

        bindAddresses
            .stream()
            .map(bootstrap::bind)
            .map(ShipServer::wrapInRuntimeException)
            .map(ChannelFuture::channel)
            .forEach(serverChannels::add);

        boundSocketAddresses = serverChannels.stream()
            .map(Channel::localAddress)
            .map(InetSocketAddress.class::cast)
            .collect(Collectors.toSet());

        log.info(
            "bound addresses for {}:\n\t{}",
            logPrefix,
            boundSocketAddresses.stream()
                .map(ShipUtilities::beautify)
                .collect(Collectors.joining("\n\t"))
        );
    }

    private static ChannelFuture wrapInRuntimeException(ChannelFuture toSync) {
        try {
            return toSync.sync();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return a copy of all local bound socket addresses of this server
     */
    public Set<InetSocketAddress> getBoundSocketAddresses() {
        return boundSocketAddresses;
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
            serverChannels.close().sync();

            // syncing graceful netty shutdowns apparently creates deadlocks
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
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
                log.warn("{}: ClientConnectedListener not set", logPrefix);
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

    public ShipNodeImpl getSocketListener() {
        return shipNode;
    }
}
