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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import org.openmuc.jeebus.ship.node.ShipNodeContext;

public class ShipServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslContext;

    // will be forwarded to ShipServerHandler
    private final ShipNodeContext nodeContext;
    private final ShipServer server;

    public ShipServerInitializer(
        SslContext sslContext,
        ShipNodeContext nodeContext,
        ShipServer server
    ) {
        this.sslContext = sslContext;

        this.nodeContext = nodeContext;
        this.server = server;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(sslContext.newHandler(ch.alloc()));
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(1024));
        pipeline.addLast(new ShipServerHandler(nodeContext, server));
    }
}
