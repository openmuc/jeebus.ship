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
import org.openmuc.jeebus.ship.api.ClientConnectedCallBack;
import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.api.ShipNodeConfiguration;
import org.openmuc.jeebus.ship.api.cert.CertificateStoreException;
import org.openmuc.jeebus.ship.message.connectionclose.ConnectionCloseReasonType;
import org.openmuc.jeebus.ship.node.service.ServiceRegistry;
import org.openmuc.jeebus.ship.node.service.TxtRecord;
import org.openmuc.jeebus.ship.node.websocket.WebSocketHandler;
import org.openmuc.jeebus.ship.node.websocket.client.ShipClient;
import org.openmuc.jeebus.ship.node.websocket.client.ShipClientHandler;
import org.openmuc.jeebus.ship.node.websocket.server.ShipServer;
import org.openmuc.jeebus.ship.node.websocket.server.ShipServerHandler;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.ServiceInfo;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.openmuc.jeebus.ship.node.KeyManagement.encodeSkiAsString;
public class ShipNodeImpl {

    protected static final Logger log = LoggerFactory.getLogger(ShipNodeImpl.class);
    private final String ownShipId;

    private final List<ShipServer> servers
        = Collections.synchronizedList(new ArrayList<>());

    private final List<ShipClient> clients
        = Collections.synchronizedList(new ArrayList<>());

    private KeyManagement keyManagement;

    private boolean autoAccept;

    private Future<?> autoAcceptTimeout;

    private final SslContextFactory sslContextFactory;

    private final Configuration config;

    private final TxtRecord ownTxt;

    private final ServiceInfo ownService;

    private ServiceRegistry serviceRegistry;

    private final ConnectionHandler connHandler;

    private ClientConnectedCallBack clientConnectedListener;

    /**
     * sets up a SHIP node
     *
     * @param nodeConfig
     *     SHIP node configuration parameters
     * @param connHandler
     *     connection handler to handle all interactions with SHIP peer
     */
    public ShipNodeImpl(
        ShipNodeConfiguration nodeConfig,
        ConnectionHandler connHandler
    ) {

        String serviceId = nodeConfig.getServiceId();
        if (serviceId.getBytes().length > 63) {
            throw new IllegalArgumentException(
                "service id should not be longer than 63 bytes");
        }

        ownShipId = nodeConfig.getServiceId();

        try {
            this.keyManagement = new KeyManagement(
                nodeConfig.getCertificateStorage(),
                nodeConfig.getDistinguishedName(),
                nodeConfig.getServiceId(),
                nodeConfig.getCertificateValidityInDays()
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

        config = new Configuration();

        sslContextFactory = new SslContextFactory();

        String wssPath = nodeConfig.getWssPath();
        String path;
        if (wssPath == null || wssPath.isEmpty()) {
            path = "/";
        }
        else if (!wssPath.startsWith("/")) {
            path = "/" + wssPath;
        }
        else {
            path = wssPath;
        }

        int port = nodeConfig.getPort();

        if (!nodeConfig.isClientOnly()) {
            createServer(port, path, nodeConfig.isKeepAlive());
        }

        String serviceDomain = nodeConfig.getServiceDomain().endsWith(".")
            ? nodeConfig.getServiceDomain()
            : nodeConfig.getServiceDomain() + ".";

        String serviceType = "_ship._tcp." + serviceDomain;
        String mdnsName = String.join(".", serviceId, serviceDomain);
        serviceRegistry = new ServiceRegistry(
            nodeConfig.getIpAddresses(),
            mdnsName,
            serviceType,
            connHandler
        );

        String ski = encodeSkiAsString(keyManagement.getOwnSki());
        ownTxt = new TxtRecord(
            serviceId,
            wssPath,
            ski,
            false,
            "Fraunhofer ISE",
            "jEEBus",
            "jEEBus"
        );

        ownService = ServiceRegistry.createServiceInfo(
            serviceType,
            nodeConfig.getServiceInstance(),
            port,
            ownTxt
        );

        try {
            serviceRegistry.registerService(ownService);
        }
        catch (IOException e) {
            log.error("Exception while registering service: {}", e.getMessage());
        }
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

    public ShipClient createClient(URI serverUri) {
        try {
            SslContext clientCtx = sslContextFactory.generateClientSslContext(keyManagement.getCert());
            ShipNodeContext nodeCtx = new ShipNodeContext(config, ownShipId);
            nodeCtx.setConnHandler(connHandler);
            ShipClient client = new ShipClient(
                clientCtx,
                serverUri,
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
        catch (SSLException e) {
            log.error("Exception while creating a SHIP client: ", e);
        }
        return null;
    }

    public ShipServer createServer(int port, String wssPath, boolean keepAlive) {
        try {
            SslContext sslCtx = sslContextFactory.generateServerSslContext(keyManagement.getCert());
            ShipNodeContext nodeCtx = new ShipNodeContext(config, ownShipId);
            nodeCtx.setConnHandler(connHandler);
            ShipServer server = new ShipServer(
                sslCtx,
                port,
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
     * activates auto accept mode
     */
    public synchronized void autoAcceptMode() {
        boolean registerFlag = Boolean.parseBoolean(ownService.getPropertyString(
            "register"));
        if (!registerFlag) {
            setRegisterFlagInTxt(true);
        }
        if (autoAcceptTimeout == null) {
            int autoAcceptWindow = getConfig().getAutoAcceptWindow();
            log.info("SHIP node starting auto accept mode, it will last for {} seconds",
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

    private synchronized void setRegisterFlagInTxt(boolean registerFlag) {
        ownTxt.setRegister(registerFlag);
        ownService.setText(ownTxt.getTxtRecordProps());
        try {
            serviceRegistry.changeService(ownService, ownService);
        }
        catch (IOException e) {
            log.error(e.getMessage());
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
            setRegisterFlagInTxt(false);
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

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
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

    public void setKeyManagement(KeyManagement keyManagement) {
        this.keyManagement = keyManagement;
    }

    public Configuration getConfig() {
        return this.config;
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

    public ClientConnectedCallBack getClientConnectedListener() {
        return clientConnectedListener;
    }

    public void setClientConnectedListener(ClientConnectedCallBack listener) {
        this.clientConnectedListener = listener;
    }

    public boolean isAutoAccept() {
        return autoAccept;
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
