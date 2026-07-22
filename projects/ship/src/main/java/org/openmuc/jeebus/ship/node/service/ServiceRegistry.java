/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node.service;

import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.api.ShipService;
import org.openmuc.jeebus.ship.node.ShipConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ServiceRegistry implements ServiceListener, AutoCloseable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The JmDNS instances for this service registry. There will be one such instance
     * for every network interface where the SHIP node publishes its service.
     */

    private final Map<InetAddress, JmDNS> addressJmDNSMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService interfaceScanner =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mdns-interface-scanner");
            t.setDaemon(true);
            return t;
        });

    private final String serviceType;

    private final ConnectionHandler connHandler;

    private final Set<String> loggedServices = new HashSet<>();

    private final String hostname;

    private final ShipConfig config;

    private TxtRecord ownTxt;
    private Set<InetSocketAddress> boundSocketAddresses;

    private static final long SCAN_INTERVAL_SEC = 10;

    /**
     * creates a JmDNS instances and a service listener
     *
     * @param config
     *     the SHIP config
     * @param connHandler
     *     connection handler to handle all interactions with the SHIP peer
     */
    public ServiceRegistry(
        ShipConfig config,
        ConnectionHandler connHandler
    ) {
        // set discovery TTL to 2 minutes as per SHIP specification
        System.setProperty("net.dns.ttl", "120");

        this.config = config;

        this.hostname = String.join(
            ".",
            config.getId(),
            config.getmDnsDomain()
        );

        this.serviceType = "_ship._tcp." + config.getmDnsDomain();

        this.connHandler = connHandler;

        config.getServerBindAddresses()
            .stream()
            .map(InetSocketAddress::getAddress)
            .forEach(this::initiateServiceFor);

        interfaceScanner.scheduleWithFixedDelay(
            this::scanForNewInterfaces,
            SCAN_INTERVAL_SEC,
            SCAN_INTERVAL_SEC,
            TimeUnit.SECONDS
        );
    }

    private JmDNS initiateServiceFor(InetAddress address) {
        return addressJmDNSMap.computeIfAbsent(address, this::safelyInitiateService);
    }

    private JmDNS safelyInitiateService(InetAddress address) {
        try {
            JmDNS jmdns = JmDNS.create(address, hostname);
            jmdns.addServiceListener(serviceType, this);
            log.debug("mDNS service initiated for new address {}", address);
            return jmdns;
        }
        catch (IOException e) {
            log.error(
                "There was an exception while initiating mDNS for {}."
                    + " mDNS sevice discovery will not be available for this"
                    + " address.",
                address,
                e
            );
            return null;
        }
    }

    private void scanForNewInterfaces() {
        try {
            Set<InetAddress> liveAddresses = currentInterfaceAddresses();

            // add JmDNS for addresses we don't have yet
            for (InetAddress address : liveAddresses) {
                if (!addressJmDNSMap.containsKey(address)) {
                    JmDNS jmdns = initiateServiceFor(address);
                    if (jmdns != null) {
                        registerOnNewInterface(address);
                    }
                }
            }

            removeStaleInterfaces(liveAddresses);
        }
        catch (Exception e) {
            log.warn("Error while scanning for new network interfaces", e);
        }
    }

    private Set<InetAddress> currentInterfaceAddresses() throws SocketException {
        Set<InetAddress> result = new HashSet<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                result.addAll(Collections.list(networkInterface.getInetAddresses()));
            }
        }
        return result;
    }

    /**
     * When a new interface comes up, register the service on it using the same port
     * we used for the already-bound addresses (if any).
     */
    private void registerOnNewInterface(InetAddress newAddress) {
        if (ownTxt == null || boundSocketAddresses == null
            || boundSocketAddresses.isEmpty()) {
            // we haven't been asked to register a service yet
            return;
        }

        // reuse the port of an existing bound address
        int port = boundSocketAddresses.iterator().next().getPort();
        InetSocketAddress newSocketAddress = new InetSocketAddress(newAddress, port);

        InetSocketAddress registered = registerService(newSocketAddress);
        if (registered != null) {
            boundSocketAddresses.add(registered);
            log.info("Registered mDNS service on newly available interface {}",
                newSocketAddress);
        }
    }

    private void removeStaleInterfaces(Set<InetAddress> liveAddresses) {
        for (InetAddress address : new HashSet<>(addressJmDNSMap.keySet())) {
            if (!liveAddresses.contains(address)) {
                JmDNS jmdns = addressJmDNSMap.remove(address);

                if (jmdns != null) {
                    log.debug("Interface {} disappeared, closing its JmDNS", address);
                    try (jmdns) {
                        jmdns.unregisterAllServices();
                        jmdns.removeServiceListener(serviceType, this);
                    }
                    catch (IOException e) {
                        log.warn("Error closing JmDNS for {}", address, e);
                    }
                    if (boundSocketAddresses != null) {
                        boundSocketAddresses.removeIf(socket -> Objects.equals(
                            socket.getAddress(),
                            address
                        ));
                    }
                }
            }
        }
    }

    /**
     * creates service info, typically used to register a service
     *
     * @param port
     *     the port the service is bound to
     * @return ServiceInfo object which holds the compressed information passed in
     * parameters
     */
    public ServiceInfo createServiceInfo(int port, TxtRecord txtRecord) {
        return ServiceInfo.create(
            serviceType,
            config.getmDnsServiceInstance(),
            port,
            0,
            0,
            txtRecord.getTxtRecordProps()
        );
    }

    /**
     * lists all identified services found across all active network interfaces.
     *
     * @return identified services
     */
    public Set<ServiceInfo> listServices() {
        return addressJmDNSMap.values()
            .stream()
            .flatMap(j -> Arrays.stream(j.list(serviceType)))
            .collect(Collectors.toSet());
    }

    /**
     * register all services in own JmDNS instance, other active JmDNS listening for
     * the same service type will identify this service once registered
     *
     * @param boundSocketAddresses
     *     the addresses to register services on
     */
    public void initiateServices(
        String ski,
        Set<InetSocketAddress> boundSocketAddresses
    ) {
        ownTxt = createTxt(ski);

        validateTxt(ownTxt);

        // we consider the result of the registration once to get rid of troubled
        // network interfaces
        this.boundSocketAddresses = registerAllServices(boundSocketAddresses);
    }

    TxtRecord createTxt(String ski) {
        return new TxtRecord(
            config.getId(),
            config.getWssPath(),
            ski,
            config.getAutoAcceptEnabled(),
            config.getBrand(),
            config.getType(),
            config.getModel()
        );
    }

    void validateTxt(TxtRecord txtRecord) {
        /* As the actual ports are revealed later and only if needed, let's just
         * assume a five digit port to test the worst case */
        ServiceInfo serviceInfo = createServiceInfo(99999, txtRecord);

        if (serviceInfo.getTextBytes().length > 400) {
            throw new IllegalArgumentException(
                "According to SHIP:7.3.2, the TXT record SHALL NOT exceed 400 "
                    + "bytes in length."
            );
        }
    }

    private Set<InetSocketAddress> registerAllServices(
        Set<InetSocketAddress> boundSocketAddresses
    ) {
        return boundSocketAddresses
            .stream()
            .map(this::registerService)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private InetSocketAddress registerService(InetSocketAddress socketAddress) {
        JmDNS boundJmDns = addressJmDNSMap.get(socketAddress.getAddress());
        if (boundJmDns == null) {
            return null;
        }
        try {
            boundJmDns.registerService(
                createServiceInfo(socketAddress.getPort(), ownTxt)
            );
            return socketAddress;
        }
        catch (IOException e) {
            log.warn(
                "There was an exception while registering an mDNS service."
                    + " Skipping address {}",
                socketAddress,
                e
            );
            return null;
        }
    }

    private void unregisterAllServices() {
        for (JmDNS jmdns : addressJmDNSMap.values()) {
            jmdns.unregisterAllServices();
        }
    }

    public void toggleRegisterFlag() {
        if (boundSocketAddresses != null) {
            unregisterAllServices();
            ownTxt.setRegister(!ownTxt.getRegister());
            registerAllServices(boundSocketAddresses);
        }
    }

    /**
     * unregisters all services in own JmDNS instance, removes service listener and
     * closes JmDNS instance
     *
     * @throws IOException
     *     if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        log.info("shutting down mDNS service registry");
        interfaceScanner.shutdownNow();   // stop the scanner first

        CompletableFuture.allOf(
            addressJmDNSMap.values().stream()
                .map(jmdns -> CompletableFuture.runAsync(() -> {
                    try (jmdns) {
                        log.debug("shutting down JmDNS for {}", jmdns.getInetAddress());
                        jmdns.unregisterAllServices();
                        jmdns.removeServiceListener(serviceType, this);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })).toArray(CompletableFuture[]::new)
        ).join();

        addressJmDNSMap.clear();
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        // Required to force serviceResolved to be called again
        // (after the first search)
        event.getDNS().requestServiceInfo(event.getType(), event.getName());
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        log.trace("service removed: {}", event.getName());
        if (connHandler != null) {
            connHandler.serviceRemoved(new ShipService(event));
        }
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        if (hasProperties(event)) {
            ShipService service = new ShipService(event);

            if (loggedServices.contains(service.getInstance())) {
                log.trace(
                    "mdns service resolved again: {}",
                    service
                );
            }
            else {
                loggedServices.add(service.getInstance());

                log.info("new mDNS service resolved: {}", service.getInstance());

                log.debug(service.toString());
            }

            connHandler.serviceAdded(service);
        }
    }

    private static boolean hasProperties(ServiceEvent event) {
        return event.getInfo().getPropertyNames().hasMoreElements();
    }

}
