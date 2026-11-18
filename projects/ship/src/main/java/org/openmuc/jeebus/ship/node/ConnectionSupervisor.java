/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node;

import io.netty.handler.ssl.SslContext;
import org.openmuc.jeebus.ship.api.*;
import org.openmuc.jeebus.ship.node.websocket.client.ShipClient;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.openmuc.jeebus.ship.api.AutoConnectPolicy.ALL;
import static org.openmuc.jeebus.ship.api.AutoConnectPolicy.TRUSTED;

public class ConnectionSupervisor implements ConnectionHandler {

    private Map<String, ShipPeer> knownPeers = new HashMap<>();
    private AutoConnectPolicy autoConnect;
    private KeyManagement trustStore;

    public ConnectionSupervisor(
        AutoConnectPolicy autoConnect,
        KeyManagement trustStore
    ) {
        this.autoConnect = autoConnect;
        this.trustStore = trustStore;
    }

    @Override
    public void onMessageReceived(
        byte[] fullMsg,
        byte[] payload,
        ShipConnectionInterface shipConn
    ) {

    }

    @Override
    public void onDisconnect(
        DisconnectReason reason,
        ShipConnectionInterface shipConn
    ) {

    }

    @Override
    public void serviceAdded(ShipService service) {

        String peerSki = service.getSki();

        knownPeers.computeIfAbsent(peerSki, ShipPeer::new)
            .addService(service);

        if (autoConnect == ALL
            || autoConnect == TRUSTED
            && trustStore.trusts(peerSki)
        ) {
            knownPeers.computeIfAbsent(peerSki, ShipPeer::new)
                .getActiveConnection()
                .ifAbsent()
        }
    }

    @Override
    public void serviceRemoved(ShipService service) {
        knownPeers.computeIfPresent(
            service.getSki(),
            (ski, shipPeer) -> shipPeer.removeService(service)
        );

    }

    @Override
    public void clientConnected(ShipConnectionInterface connection) {
        String peerSki = connection.getRemoteSki();

        knownPeers
            .computeIfAbsent(peerSki, ShipPeer::new)
            .setSetActiveConnection(connection);
    }

    public ShipClient createClient(
        InetSocketAddress address,
        String path,
        String expectedId,
        String expectedSki
    ) {
        try {
            SslContext clientCtx = sslContextFactory.generateClientSslContext(
                keyManagement.getCert()
            );
            ShipNodeContext nodeCtx = new ShipNodeContext(
                this.keyManagement,
                nodeConfig.getId(),
                expectedId,
                expectedSki
            );
            nodeCtx.setConnHandler(connHandler);
            ShipClient client = new ShipClient(
                clientCtx,
                address,
                path,
                nodeCtx,
                this
            );
            addClient(client);
            return client;
        }
        catch (SSLException | URISyntaxException e) {
            log.error("Exception while creating a SHIP client: ", e);
        }
        return null;
    }
}
