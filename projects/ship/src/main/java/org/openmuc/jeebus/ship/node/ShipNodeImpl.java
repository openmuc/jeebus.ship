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
import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.api.cert.CertificateStoreException;
import org.openmuc.jeebus.ship.message.connectionclose.ConnectionCloseReasonType;
import org.openmuc.jeebus.ship.node.service.ServiceRegistry;
import org.openmuc.jeebus.ship.node.websocket.WebSocketHandler;
import org.openmuc.jeebus.ship.node.websocket.client.ShipClient;
import org.openmuc.jeebus.ship.node.websocket.client.ShipClientHandler;
import org.openmuc.jeebus.ship.node.websocket.server.ShipServer;
import org.openmuc.jeebus.ship.node.websocket.server.ShipServerHandler;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openmuc.jeebus.ship.node.ShipNodeParameters.USER_VERIFIED_TRUST_LEVEL;

public class ShipNodeImpl {

    protected static final Logger log = LoggerFactory.getLogger(ShipNodeImpl.class);

    private final Optional<ShipServer> server;

    private final List<ShipClient> clients
        = Collections.synchronizedList(new ArrayList<>());

    private final Set<String> currentRemoteSkis = new ConcurrentSkipListSet<>();

    private final ShipConfig nodeConfig;

    private final KeyManagement keyManagement;

    private final NetworkInterfaceScanner networkInterfaceScanner;

    private boolean autoAccept;

    private Future<?> autoAcceptTimeout;

    private final SslContextFactory sslContextFactory;

    private final ServiceRegistry serviceRegistry;

    private final ConnectionHandler connHandler;

    /**
     * sets up a SHIP node
     *
     * @param nodeConfig
     *     SHIP node configuration parameters
     * @param connHandler
     *     connection handler to handle all interactions with SHIP peer
     */
    public ShipNodeImpl(
        ShipConfig nodeConfig,
        ConnectionHandler connHandler
    ) {
        this.nodeConfig = nodeConfig;

        try {
            this.keyManagement = new KeyManagement(
                nodeConfig.getCertificateStorage(),
                nodeConfig.getCertificateDistinguishedName(),
                nodeConfig.getId(),
                nodeConfig.getCertificateValidity()
            );

            log.info(
                "Key Management initialized. SKI of this node is {}",
                keyManagement.getOwnSki()
            );
        }
        catch (CertificateStoreException e) {
            log.error("Exception while loading or creating key store: ", e);
            throw new RuntimeException(e);
            // if we don't exit here, there would be an NPE later anyway
            // best to fail-fast
        }

        nodeConfig.getTrustedSkis()
            .forEach(ski -> keyManagement.addTrustedSki(ski, USER_VERIFIED_TRUST_LEVEL));

        this.connHandler = connHandler;

        // disable TLS client initiated renegotiation as per SHIP specification,
        // server initiated renegotiation is not supported by netty as of version 4.1
        System.setProperty("jdk.tls.rejectClientInitiatedRenegotiation", "true");

        sslContextFactory = new SslContextFactory();

        // Even if we do not start a server to announce via mDNS,
        // we might still want to listen in.
        serviceRegistry = new ServiceRegistry(
            nodeConfig,
            connHandler
        );

        if (nodeConfig.getServerEnabled()) {
            this.server = Optional.of(createServer(
                nodeConfig.getServerBindAddresses(),
                nodeConfig.getWssPath(),
                nodeConfig.isKeepAlive()
            ));

            serviceRegistry.initiateTxt(
                getOwnSki()
            );

            if (nodeConfig.getAutoAcceptEnabled()) {
                enableAutoAcceptMode();
            }
        }
        else {
            this.server = Optional.empty();
        }

        networkInterfaceScanner = new NetworkInterfaceScanner(
            serviceRegistry,
            this,
            nodeConfig
        );
    }

    public Set<WebSocketHandler> getAllWebSocketHandlers() {
        return Stream.concat(
            server.stream()
                .map(ShipServer::getHandlers)
                .flatMap(Collection::stream),
            clients.stream()
                .map(ShipClient::getHandler)
        ).collect(Collectors.toSet());
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

    public ShipServer createServer(
        Set<InetSocketAddress> bindAddresses,
        String wssPath,
        boolean keepAlive
    ) {
        try {
            SslContext sslCtx = sslContextFactory.generateServerSslContext(
                keyManagement.getCert()
            );
            ShipNodeContext nodeCtx = new ShipNodeContext(
                this.keyManagement,
                nodeConfig.getId()
            );
            nodeCtx.setConnHandler(connHandler);
            return new ShipServer(
                sslCtx,
                bindAddresses,
                wssPath,
                nodeCtx,
                this,
                keepAlive
            );
        }
        catch (InterruptedException e) {
            log.error("Exception while creating a SHIP client: ", e);
            Thread.currentThread().interrupt();
        }
        catch (SSLException e) {
            log.error("Exception while creating a SHIP client: ", e);
        }
        return null;
    }

    /**
     * enables the auto accept mode
     */
    public synchronized void enableAutoAcceptMode() {
        if (autoAcceptTimeout == null) {
            serviceRegistry.setRegisterFlag(true);

            log.info(
                "SHIP node starting auto accept mode, it will last for {} seconds",
                ShipNodeParameters.AUTO_ACCEPT_WINDOW
            );
            autoAccept = true;
            ScheduledExecutorService es
                = Executors.newSingleThreadScheduledExecutor();
            autoAcceptTimeout = es.schedule((Runnable) this::consumeAutoAccept,
                ShipNodeParameters.AUTO_ACCEPT_WINDOW,
                TimeUnit.SECONDS
            );
            es.shutdown();
        }
        else {
            log.warn("SHIP node is already in auto accept mode");
        }
    }

    /**
     * synchronized method to check if auto accept mode is running. Cancels the
     * timeout if it is and sets auto accept to false.
     *
     * @return {@code true} if SHIP node was in auto accept mode, else returns
     * {@code false}
     */
    public synchronized boolean consumeAutoAccept() {
        if (autoAccept) {
            autoAccept = false;
            autoAcceptTimeout.cancel(true);
            autoAcceptTimeout = null;
            serviceRegistry.setRegisterFlag(false);
            return true;
        }
        return false;
    }

    public void stopServer() {
        this.server.ifPresent(ShipServer::stop);
    }

    public void stopAllClients() {
        // clone the list first to avoid ConcurrentModificationException
        List<ShipClient> clients = new ArrayList<>(this.clients);
        clients
            .stream()
            .filter(Objects::nonNull)
            .peek(this::removeClient)
            .forEach(ShipClient::stop);
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public Optional<ShipServer> getServer() {
        return server;
    }

    /**
     * @return an unmodifiable view of the client list
     */
    public List<ShipClient> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public void addClient(ShipClient client) {
        clients.add(client);
    }

    public boolean removeClient(ShipClient client) {
        removeCurrentRemoteSki(client.getHandler().getPeerSki());
        return clients.remove(client);
    }

    public KeyManagement getKeyManagement() {
        return keyManagement;
    }

    public ConnectionHandler getConnHandler() {
        return connHandler;
    }

    public void setConnHandler(ConnectionHandler connHandler) {
        server.ifPresent(server -> server.setConnHandler(connHandler));

        // Create a copy of the clients list to avoid holding the synchronized list lock during iteration
        List<ShipClient> clientsCopy = new ArrayList<>(clients);
        for (ShipClient client : clientsCopy) {
            client.setConnHandler(connHandler);
        }

        serviceRegistry.setConnHandler(connHandler);

    }

    /**
     * @param peerSki the SKI to add
     * @return {@link Set#add(Object)}
     */
    public boolean addCurrentRemoteSki(String peerSki) {
        return currentRemoteSkis.add(peerSki);
    }

    public void removeCurrentRemoteSki(String peerSki) {
        currentRemoteSkis.remove(peerSki);
    }

    public String getOwnSki() {
        return keyManagement.getOwnSki();
    }
}
