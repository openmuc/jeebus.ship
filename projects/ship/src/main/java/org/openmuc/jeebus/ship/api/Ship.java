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
import org.openmuc.jeebus.ship.node.ShipNodeParameters;
import org.openmuc.jeebus.ship.node.websocket.WebSocketHandler;
import org.openmuc.jeebus.ship.node.websocket.client.ShipClient;
import org.openmuc.jeebus.ship.node.websocket.client.ShipClientHandler;
import org.openmuc.jeebus.ship.node.websocket.server.ShipServer;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import static org.openmuc.jeebus.ship.node.ShipNodeParameters.USER_VERIFIED_TRUST_LEVEL;
import static org.openmuc.jeebus.ship.node.ShipNodeParameters.WSS_HANDSHAKE_TIMEOUT;
import static org.openmuc.jeebus.ship.util.ShipUtilities.beautify;
import static org.openmuc.jeebus.ship.util.ShipUtilities.safelyParseSocketAddress;

public class Ship implements ShipInterface, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Ship.class);
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

    @Override
    public ShipConnectionInterface openConnection(String ipAddr) {
        try {
            return openConnection(
                safelyParseSocketAddress(ipAddr),
                "ship"
            ).get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<ShipConnectionInterface> openConnection(
        InetSocketAddress socket,
        String path
    ) {
        return openConnection(
            socket,
            path,
            null,
            null
        );
    }

    /**
     * {@inheritDoc}
     *
     * @implNote If a connection with the given SHIP node already exists, that
     * {@link ShipConnectionInterface} is returned immediately in a completed
     * Future. The reason for this behavior is that we want to avoid
     * double connections, as they are always messy and unreliable to handle.
     */
    @Override
    public CompletableFuture<ShipConnectionInterface> openConnection(
        InetSocketAddress socket,
        String path,
        String expectedShipId,
        String expectedSki
    ) {
        assertNodeAvailable();

        if (expectedSki != null && !trusts(expectedSki)) {
             log.debug("Opening a connection to a device that is not trusted yet.");
        }

        return getExistingConnection(socket, expectedShipId)
            .map(connection -> {
                log.info(
                    "Reusing existing connection to {} ({})",
                    expectedShipId,
                    beautify(socket)
                );
                return CompletableFuture.completedFuture(connection.getApiShipConnection());
            })
            .orElseGet(() -> establishNewClientConnection(
                socket,
                path,
                expectedShipId,
                expectedSki
            ));
    }

    private Optional<ShipConnectionImpl> getExistingConnection(
        InetSocketAddress socket,
        String shipId
    ) {
        assertNodeAvailable();
        return node
            .getAllWebSocketHandlers()
            .stream()
            .filter(handler ->
                Objects.equals(
                    Optional
                        .ofNullable(handler.getShipConnection())
                        .map(ShipConnectionImpl::getRemoteId)
                        // invalid SKI so we never compare null to null
                        .orElse("invalid"),
                    shipId
                ) || Objects.equals(handler.getRemoteSocketAddress(), socket)
            )
            .map(WebSocketHandler::getShipConnection)
            .filter(Objects::nonNull)
            .filter(Predicate.not(ShipConnectionImpl::isConnectionCloseState))
            .findFirst();
    }

    private CompletableFuture<ShipConnectionInterface> establishNewClientConnection(
        InetSocketAddress socket,
        String path,
        String expectedId,
        String expectedSki
    ) {
        return CompletableFuture
            .supplyAsync(() -> doClientHandshake(
                socket,
                path,
                expectedId,
                expectedSki
            ))
            .thenCompose(client -> client
                .getHandler()
                .getConnection()
                .start()
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        node.removeClient(client);
                    }
                })
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    client.stop();
                    log.error(
                        "Failed to establish new client connection:",
                        throwable
                    );
                }
            }));
    }

    private ShipClient doClientHandshake(
        InetSocketAddress socket,
        String path,
        String expectedId,
        String expectedSki
    ) {
        try {
            ShipClient client = node.createClient(
                socket,
                path,
                expectedId,
                expectedSki
            );
            ShipClientHandler clientHandler = client.getHandler();

            if (!clientHandler.awaitWssHandshakeCompletion(WSS_HANDSHAKE_TIMEOUT)) {
                throw new IllegalStateException(
                    "WSS Handshake took more than "+WSS_HANDSHAKE_TIMEOUT+" seconds."
                );
            }

            node.addClient(client);

            return client;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
    }

    public String getOwnSki() {
        assertNodeAvailable();
        return node.getKeyManagement().getOwnSki();
    }

    /**
     * @param remoteSki
     *     the Subject Key Identifier (SKI) of the remote SHIP note in question
     * @return true if the trust level for the given SKI is &ge;
     * {@link ShipNodeParameters#MINIMAL_TRUST_LEVEL}.
     */
    public boolean trusts(String remoteSki) {
        return node.getKeyManagement().trusts(remoteSki);
    }

    /**
     * adding a SKI authenticates the SHIP node using that SKI. The SKI of other SHIP
     * nodes is typically communicated through the {@link ConnectionHandler} or
     * manually acquired through {@link ShipConnectionInterface}
     *
     * @param ski
     *     the ski to add to the trusted SKIs
     */
    public void addTrustedSki(String ski) {
        assertNodeAvailable();
        if (KeyManagement.isValidSki(ski)) {
            node.getKeyManagement().addTrustedSki(ski, USER_VERIFIED_TRUST_LEVEL);
            synchronized (node.getServer()) {
                node.getServer()
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
     * {@code false}, for example because the trusted SKI list did not contain the
     * passed ski
     */
    public boolean removeTrustedSki(String ski) {
        if (node.getKeyManagement().removeTrustedSki(ski)) {
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
     * clients have a time window of 1 to 120 seconds for the connection The default
     * value for the window is 60 seconds. If the time window passed without any
     * client connecting, the auto-accept-mode will be turned off.
     *
     * @deprecated since 2.3.0. Usage of the auto accept mode is discouraged by the
     * EEBus Initiative and most stack implementers. Experience has shown even one
     * device in auto accept mode makes setting up working EEBus networks hard and
     * unreliable. It may be removed in future SHIP specification versions.
     */
    @Deprecated(since = "2.3.0")
    public void setAutoAcceptMode() {
        assertNodeAvailable();
        node.enableAutoAcceptMode();
    }

    @Override
    public void setConnectionHandler(ConnectionHandler connectionHandler) {
        assertNodeAvailable();
        node.setConnHandler(connectionHandler);
    }

    /**
     * returns a set with all detected services, including own service
     *
     * @return the set with all detected services
     * @deprecated since 3.0.0. Please use {@link Ship#getCurrentServices}
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public Set<ServiceInfo> getServices() {
        assertNodeAvailable();
        return node.getServiceRegistry().listServices();
    }

    /**
     * @return a Set containing all resolved ShipServices except this node's
     * servcices
     */
    public Set<ShipService> getCurrentServices() {
        assertNodeAvailable();
        return node.getServiceRegistry().getCurrentServices();
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
        node.stopServer();
        log.info("SHIP was shut down");
    }

    private void assertNodeAvailable() {
        if (node == null) {
            throw new IllegalStateException("Ship already shut down!");
        }
    }
}
