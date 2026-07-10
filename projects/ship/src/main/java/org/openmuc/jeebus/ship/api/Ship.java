/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.api;

import org.openmuc.jeebus.ship.node.KeyManagement;
import org.openmuc.jeebus.ship.node.ShipConfig;
import org.openmuc.jeebus.ship.node.ShipNodeImpl;
import org.openmuc.jeebus.ship.node.websocket.WebSocketHandler;
import org.openmuc.jeebus.ship.node.websocket.client.ShipClient;
import org.openmuc.jeebus.ship.node.websocket.client.ShipClientHandler;
import org.openmuc.jeebus.ship.node.websocket.server.ShipServer;
import org.openmuc.jeebus.ship.shipconnection.ShipConnection;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;
import org.openmuc.jeebus.ship.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Ship implements ShipInterface, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Ship.class);
    private final NamedThreadFactory namedThreadFactory = new NamedThreadFactory(
        "jEEBus.SHIP State pool thread ");
    private ShipNodeImpl node;

    /**
     * creates a new node on construction
     *
     * @param nodeConfig
     *     the configuration to be used for the node
     * @param connHandler
     *     connection handler
     */
    public Ship(ShipConfig nodeConfig, ConnectionHandler connHandler) {
        node = new ShipNodeImpl(nodeConfig, connHandler);
    }

    /**
     * {@inheritDoc}
     *
     * @return an object representing the connection to the device/server if the
     * connection was successful, else
     * {@code null}
     */
    @Override
    public ShipConnectionInterface openConnection(String ipAddr) {
        if (node == null) {
            throw new IllegalStateException("Ship already shut down!");
        }

        Optional<ShipConnectionImpl> shipServerConnection
            = getShipServerConnection(ipAddr);

        if (shipServerConnection.isPresent()) {
            ShipConnectionInterface connApi = shipServerConnection
                .get()
                .getApiShipConn();
            log.info(
                "Reusing existing server connection to {}",
                connApi.getRemoteAddress()
            );
            return connApi;
        }

        String uriStr = "wss://" + ipAddr;
        log.info("websocket uri = {}", uriStr);

        URI uri = null;
        try {
            uri = new URI(uriStr);
        }
        catch (URISyntaxException e) {
            log.error("exception while trying to create URI: ", e);
        }

        if (uri != null) {
            ShipClient client = node.createClient(uri);
            ShipClientHandler clientHandler = client.getHandler();
            try {
                if (!clientHandler.isShipConnRdy(5)) {
                    throw new IllegalStateException(
                        "ShipConnection not ready in time");
                }
            }
            catch (InterruptedException e) {
                log.error(e.getMessage());
                Thread.currentThread().interrupt();
            }
            ShipConnectionImpl connection = clientHandler.getConnection();
            runConnectionDataPreparation(connection);
            return connection.getApiShipConn();
        }

        throw new IllegalArgumentException(
            "could not create a client to the ip address: " + ipAddr);
    }

    private Optional<ShipConnectionImpl> getShipServerConnection(String ipAddr) {
        if (node == null) {
            throw new IllegalStateException("Ship already shut down!");
        }
        return node
            .getServers()
            .stream()
            .map(ShipServer::getHandlers)
            .flatMap(Collection::stream)
            .filter(handler -> handler.getRemoteIpAndPort().startsWith(ipAddr))
            .map(WebSocketHandler::getShipConnection)
            .findAny();
    }

    /**
     * run the states in connection data preparation, this method is non-blocking and
     * the states will run asynchronously
     *
     * @param connection
     *     the connection that should run the connection data preparation states
     */
    public void runConnectionDataPreparation(ShipConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection object should not be null");
        }
        ExecutorService exec = Executors.newSingleThreadExecutor(namedThreadFactory);
        exec.execute(connection::initState);
        exec.shutdown();
    }

    public String getOwnSki() {
        if (node == null) {
            throw new IllegalStateException("Ship already shut down!");
        }
        return node.getKeyManagement().getOwnSkiAsStr();
    }

    /**
     * adding a SKI authenticates the SHIP node using that SKI. The SKI of other SHIP
     * nodes is typically communicated through the {@link ConnectionHandler} or
     * manually acquired through {@link ShipConnectionInterface}
     *
     * @param ski
     *     the ski to add to the trusted SKIs
     */
    public synchronized void addTrustedSki(String ski) {
        if (node == null) {
            throw new IllegalStateException("Ship already shut down!");
        }
        if (ski != null && !ski.isEmpty()) {
            KeyManagement.addTrustedSki(ski, 32);
            synchronized (node.getServers()) {
                node.getServers()
                    .stream()
                    .map(ShipServer::getHandlers)
                    .flatMap(Collection::stream)
                    .filter(handler -> Objects.equals(handler.getPeerSki(), ski))
                    .map(WebSocketHandler::getShipConnection)
                    .filter(Objects::nonNull)
                    .forEach(ShipConnectionImpl::trustCommPartner);
            }
            synchronized (node.getClients()) {
                node.getClients()
                    .stream()
                    .map(ShipClient::getHandler)
                    .filter(handler -> Objects.equals(handler.getPeerSki(), ski))
                    .map(ShipClientHandler::getConnection)
                    .filter(Objects::nonNull)
                    .forEach(ShipConnectionImpl::trustCommPartner);
            }
        }
    }

    /**
     * tries to remove a ski from the trusted SKIs
     *
     * @param ski
     *     the ski to remove
     * @return {@code true} if the ski was removed successfully, otherwise
     * {@code false}, for example because
     * the trusted SKI list did not contain the passed ski
     */
    public boolean removeTrustedSki(String ski) {
        if (KeyManagement.removeTrustedSki(ski)) {
            // this is what it would look like to also distrust all nodes with that SKI
            // synchronized (node.getServers()) {
            //     node.getServers()
            //         .stream()
            //         .map(ShipServer::getHandlers)
            //         .flatMap(Collection::stream)
            //         .filter(handler -> Objects.equals(handler.getPeerSki(), ski))
            //         .map(WebSocketHandler::getConnection)
            //         .filter(Objects::nonNull)
            //         .forEach(ShipConnection::distrustCommPartner);
            // }
            // synchronized (node.getClients()) {
            //     node.getClients()
            //         .stream()
            //         .map(ShipClient::getHandler)
            //         .filter(handler -> Objects.equals(handler.getPeerSki(), ski))
            //         .map(ShipClientHandler::getConnection)
            //         .filter(Objects::nonNull)
            //         .forEach(ShipConnection::distrustCommPartner);
            // }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Sets the node to auto-accept-mode. Authentication can be skipped that way but
     * clients have a time window of 1 to 120 seconds for the connection
     * The default value for the window is 60 seconds. If the time
     * window passed without any client connecting, the auto-accept-mode will be
     * turned off.
     * <p>
     * @deprecated since 2.3.0. Usage of the auto accept mode is discouraged by the
     * EEBus Initiative and most stack implementers. Experience has shown even one
     * device in auto accept mode makes setting up working EEBus networks hard and
     * unreliable. It may be removed in future SHIP specification versions.
     */
    @Deprecated(since = "2.3.0")
    public void setAutoAcceptMode() {
        if (node == null) {
            throw new IllegalStateException("Ship already shut down!");
        }
        node.enableAutoAcceptMode();
    }

    @Override
    public void setConnHandler(ConnectionHandler connHandler) {
        if (node == null) {
            throw new IllegalStateException("Ship already shut down!");
        }
        node.setConnHandler(connHandler);
    }

    /**
     * @param listener
     *     will be called when a new remote SHIP client connects to the server of
     *     this node.
     */
    public void setClientConnectedListener(ClientConnectedListener listener) {
        if (node == null) {
            throw new IllegalStateException("Ship already shut down!");
        }
        node.setClientConnectedListener(listener);
    }

    /**
     * returns a set with all detected services, including own service
     *
     * @return the set with all detected services
     */
    public Set<ServiceInfo> getServices() {
        if (node == null) {
            throw new IllegalStateException("Ship already shut down!");
        }
        return node.getServiceRegistry().listServices();
    }

    /**
     * shuts down service discovery, all servers and clients on the node and the node
     * itself
     *
     * @throws IOException
     *     if service discovery close unsuccessful
     */
    @Override
    public void close() throws IOException {
        ShipNodeImpl node;
        synchronized (this) {
            node = this.node;
            this.node = null;
        }
        if (node == null) {
            log.warn("shutDown() was called after already being shut down");
            return;
        }
        node.getServiceRegistry().close();
        node.stopAllClients();
        node.stopAllServers();
        log.info("SHIP was shut down");
    }
}
