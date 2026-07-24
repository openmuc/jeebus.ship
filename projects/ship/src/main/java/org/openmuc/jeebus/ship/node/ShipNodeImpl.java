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
import org.openmuc.jeebus.ship.api.ClientConnectedListener;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openmuc.jeebus.ship.node.KeyManagement.encodeSkiAsString;
public class ShipNodeImpl {

    protected static final Logger log = LoggerFactory.getLogger(ShipNodeImpl.class);

    private final List<ShipServer> servers
        = Collections.synchronizedList(new ArrayList<>());

    private final List<ShipClient> clients
        = Collections.synchronizedList(new ArrayList<>());

    private final ShipConfig nodeConfig;

    private final KeyManagement keyManagement;

    private boolean autoAccept;

    private Future<?> autoAcceptTimeout;

    private final SslContextFactory sslContextFactory;

    private final StaticConfiguration staticConfig;

    private final ServiceRegistry serviceRegistry;

    private final ConnectionHandler connHandler;

    private ClientConnectedListener clientConnectedListener;

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
                keyManagement.getOwnSkiAsStr()
            );
        }
        catch (CertificateStoreException e) {
            log.error("Exception while loading or creating key store: ", e);
            throw new RuntimeException(e);
            // if we don't exit here, there would be an NPE later anyway
            // best to fail-fast
        }

        this.connHandler = connHandler;

        // disable TLS client initiated renegotiation as per SHIP specification,
        // server initiated renegotiation is not supported by netty as of version 4.1
        System.setProperty("jdk.tls.rejectClientInitiatedRenegotiation", "true");

        staticConfig = new StaticConfiguration();

        sslContextFactory = new SslContextFactory();

        // Even if we do not start a server to announce via mDNS,
        // we might still want to listen in.
        serviceRegistry = new ServiceRegistry(
            nodeConfig,
            connHandler
        );

        if (nodeConfig.getServerEnabled()) {
            ShipServer server = createServer(
                nodeConfig.getServerBindAddresses(),
                nodeConfig.getWssPath(),
                nodeConfig.isKeepAlive()
            );

            if (nodeConfig.getAutoAcceptEnabled()) {
                enableAutoAcceptMode();
            }

            serviceRegistry.initiateServices(
                getOwnSki(),
                server.getBoundSocketAddresses()
            );
        }
    }

    public Set<WebSocketHandler> getAllWebSocketHandlers() {
        return Stream.concat(
            servers.stream()
                .map(ShipServer::getHandlers)
                .flatMap(Collection::stream),
            clients.stream()
                .map(ShipClient::getHandler)
        ).collect(Collectors.toSet());
    }

    public void closeDoubleConns(WebSocketHandler current) {
        List<ShipServerHandler> serverHandlerToClose = new ArrayList<>();
        for (ShipServer server : servers) {
            for (ShipServerHandler serverHandler : server.getHandlers()) {
                if (serverHandler.getPeerSki().equals(current.getPeerSki())
                    && !serverHandler.equals(current)) {
                    ShipConnectionImpl shipConn = serverHandler.getShipConnection();
                    if (shipConn != null) {
                        shipConn.stopStateTimeouts();
                        if (shipConn.getCde() != null) {
                            shipConn.initiateConnectionClose(
                                100,
                                ConnectionCloseReasonType.UNSPECIFIC
                            );
                        }
                    }
                    if (shipConn == null
                        || !shipConn.isConnectionCloseState()) {
                        log.info("close double conn initiated");
                        serverHandlerToClose.add(serverHandler);
                    }
                }
            }
        }
        // close after loop to avoid ConcurrentModificationException
        serverHandlerToClose.forEach(ShipServerHandler::close);

        for (ShipClient client : clients) {
            ShipClientHandler clientHandler = client.getHandler();
            if (clientHandler.getPeerSki().equals(current.getPeerSki())
                && !clientHandler.equals(current)) {
                ShipConnectionImpl shipConn = clientHandler.getConnection();
                if (shipConn.getCde() != null) {
                    shipConn.initiateConnectionClose(
                        100,
                        ConnectionCloseReasonType.UNSPECIFIC
                    );
                }
                else {
                    log.info("close double conn initiated for client");
                    clientHandler.close();
                }
            }
        }
    }

    public ShipClient createClient(InetSocketAddress address, String path) {
        try {
            SslContext clientCtx = sslContextFactory.generateClientSslContext(
                keyManagement.getCert()
            );
            ShipNodeContext nodeCtx = new ShipNodeContext(
                staticConfig,
                nodeConfig.getId()
            );
            nodeCtx.setConnHandler(connHandler);
            ShipClient client = new ShipClient(
                clientCtx,
                address,
                path,
                nodeCtx,
                this
            );
            clients.add(client);
            return client;
        }
        catch (InterruptedException e) {
            log.error("Exception while creating a SHIP client: ", e);
            Thread.currentThread().interrupt();
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
                staticConfig,
                nodeConfig.getId()
            );
            nodeCtx.setConnHandler(connHandler);
            ShipServer server = new ShipServer(
                sslCtx,
                bindAddresses,
                wssPath,
                nodeCtx,
                this,
                keepAlive
            );
            servers.add(server);
            return server;
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
            serviceRegistry.toggleRegisterFlag();

            int autoAcceptWindow = getStaticConfig().getAutoAcceptWindow();
            log.info(
                "SHIP node starting auto accept mode, it will last for {} seconds",
                autoAcceptWindow
            );
            autoAccept = true;
            ScheduledExecutorService es
                = Executors.newSingleThreadScheduledExecutor();
            autoAcceptTimeout = es.schedule((Runnable) this::consumeAutoAccept,
                autoAcceptWindow,
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
     * @return <code>true</code> if SHIP node was in auto accept mode, else returns
     * <code>false</code>
     */
    public synchronized boolean consumeAutoAccept() {
        if (autoAccept) {
            autoAccept = false;
            autoAcceptTimeout.cancel(true);
            autoAcceptTimeout = null;
            serviceRegistry.toggleRegisterFlag();
            return true;
        }
        return false;
    }

    public void stopAllServers() {
        // clone the list first to avoid ConcurrentModificationException
        List<ShipServer> servers = new ArrayList<>(this.servers);
        servers.forEach(ShipServer::stop);
    }

    public void stopAllClients() {
        // clone the list first to avoid ConcurrentModificationException
        List<ShipClient> clients = new ArrayList<>(this.clients);
        clients.forEach(ShipClient::stop);
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public List<ShipServer> getServers() {
        return servers;
    }

    public void setServers(List<ShipServer> servers) {
        synchronized (this.servers) {
            this.servers.clear();
            this.servers.addAll(servers);
        }
    }

    public List<ShipClient> getClients() {
        return clients;
    }

    public void setClient(List<ShipClient> clients) {
        synchronized (this.clients) {
            this.clients.clear();
            this.clients.addAll(clients);
        }
    }

    public void addClient(ShipClient client) {
        clients.add(client);
    }

    public KeyManagement getKeyManagement() {
        return keyManagement;
    }

    public StaticConfiguration getStaticConfig() {
        return this.staticConfig;
    }

    public ConnectionHandler getConnHandler() {
        return connHandler;
    }

    public void setConnHandler(ConnectionHandler connHandler) {
        for (ShipServer server : servers) {
            server.setConnHandler(connHandler);
        }

        for (ShipClient client : clients) {
            client.setConnHandler(connHandler);
        }

    }

    public ClientConnectedListener getClientConnectedListener() {
        return clientConnectedListener;
    }

    public void setClientConnectedListener(ClientConnectedListener listener) {
        this.clientConnectedListener = listener;
    }

    public void removeClient(ShipClient client) {
        clients.remove(client);
    }

    public void removeServer(ShipServer server) {
        servers.remove(server);
    }

    public synchronized boolean isDoubleConnection(String peerSki) {
        int matches = 0;
        synchronized (servers) {
            for (ShipServer server : servers) {
                for (ShipServerHandler handler : server
                    .getHandlers()) {
                    if (handler.getPeerSki().equals(peerSki)) {
                        matches++;
                    }
                }
            }
        }
        synchronized (clients) {
            for (ShipClient client : clients) {
                if (client.getHandler().getPeerSki().equals(peerSki)) {
                    matches++;
                }
            }
        }
        // if remote host matches the host in parameter more than once, then return true
        return matches > 1;
    }

    public String getOwnSki() {
        return encodeSkiAsString(keyManagement.getOwnSki());
    }

}
