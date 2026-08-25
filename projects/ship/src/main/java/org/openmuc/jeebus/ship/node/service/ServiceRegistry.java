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
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.openmuc.jeebus.ship.util.ShipUtilities.SCOPED_ADDRESS_ORDER;
import static org.openmuc.jeebus.ship.util.ShipUtilities.beautify;

public class ServiceRegistry implements ServiceListener, AutoCloseable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Without a custom comparator, Collection::contains fails for link-local
    // addresses with different scopes.
    private final NavigableMap<InetAddress, JmDNS> addressJmDNSMap =
        new ConcurrentSkipListMap<>(SCOPED_ADDRESS_ORDER);

    private final String serviceType;

    private ConnectionHandler connHandler;

    private final String hostname;

    private final ShipConfig config;

    private TxtRecord ownTxt;

    private Set<InetSocketAddress> boundSocketAddresses = Collections.emptySet();

    private final Set<ShipService> reportedServices = new ConcurrentSkipListSet<>();

    /**
     * creates a new ServiceRegistry, but does not initiate any listeners nor
     * register any services.
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
    }

    public void updateListeners(Set<InetAddress> addresses) {
        addressJmDNSMap
            .keySet()
            .stream()
            .filter(Predicate.not(addresses::contains))
            .forEach(this::shutDownInstance);

        addresses
            .stream()
            .filter(Predicate.not(addressJmDNSMap.keySet()::contains))
            .forEach(this::initiateListener);
    }

    private void initiateListener(InetAddress address) {
        if(
            !addressJmDNSMap.containsKey(address)
            || addressJmDNSMap.get(address) == null
        ) {
            try {
                JmDNS jmdns = JmDNS.create(address, hostname);
                jmdns.addServiceListener(serviceType, this);
                log.debug(
                    "mDNS service initiated for new address {}",
                    beautify(address)
                );
                addressJmDNSMap.put(address, jmdns);
            }
            catch (IOException e) {
                log.warn(
                    "There was an exception while initiating mDNS for {}."
                        + " mDNS sevice discovery will not be available for this"
                        + " address.",
                    beautify(address),
                    e
                );
            }
        }
    }

    private void shutDownInstance(InetAddress forAddress) {
        JmDNS jmdns = addressJmDNSMap.remove(forAddress);

        if (jmdns != null) {
            log.debug(
                "Closing JmDNS instance for {}",
                beautify(forAddress)
            );
            try (jmdns) {
                jmdns.unregisterAllServices();
                jmdns.removeServiceListener(serviceType, this);
            }
            catch (IOException e) {
                log.warn("Error closing JmDNS for {}", beautify(forAddress), e);
            }
            if (boundSocketAddresses != null) {
                boundSocketAddresses.removeIf(socket ->
                    SCOPED_ADDRESS_ORDER.compare(
                        socket.getAddress(),
                        forAddress
                    ) == 0);
            }
            Optional<ShipService> service = reportedServices.stream().filter(entry ->
                entry
                    .getSocketAddresses()
                    .stream()
                    .map(InetSocketAddress::getAddress)
                    .anyMatch(address -> SCOPED_ADDRESS_ORDER.compare(
                        address,
                        forAddress
                    ) == 0)
            ).findAny();

            if (service.isPresent() && !isUs(service.get())) {
                reportedServices.remove(service.get());
                connHandler.serviceRemoved(service.get());
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
     * @deprecated since 3.0.0. Please use {@link ServiceRegistry#getCurrentServices}
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public Set<ServiceInfo> listServices() {
        return addressJmDNSMap.values()
            .stream()
            .flatMap(j -> Arrays.stream(j.list(serviceType)))
            .collect(Collectors.toSet());
    }

    public Set<ShipService> getCurrentServices() {
        return addressJmDNSMap
            .values()
            .stream()
            .flatMap(dns -> Arrays
                .stream(dns.list(serviceType))
                .map(info -> new ShipService(info, fixLinkLocal(info, dns))))
            .filter(Predicate.not(this::isUs))
            .collect(Collectors.toSet());
    }

    public void initiateTxt(
        String ski
    ) {
        ownTxt = createTxt(ski);

        validateTxt(ownTxt);
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

    public void registerServices(
        Set<InetSocketAddress> sockets
    ) {
        this.boundSocketAddresses = sockets
            .stream()
            .map(socket -> boundSocketAddresses.contains(socket) ?
                socket :
                registerService(socket))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private InetSocketAddress registerService(InetSocketAddress socketAddress) {
        JmDNS boundJmDns = addressJmDNSMap.get(socketAddress.getAddress());
        if (boundJmDns != null) {
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
                    beautify(socketAddress),
                    e
                );
            }
        }
        return null;
    }

    private void unregisterAllServices() {
        for (JmDNS jmdns : addressJmDNSMap.values()) {
            jmdns.unregisterAllServices();
        }
    }

    /**
     * unregisters all services, removes service listeners and closes all JmDNS
     * instances
     *
     * @throws IOException
     *     if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        log.info("shutting down mDNS service registry");

        CompletableFuture.allOf(
            addressJmDNSMap.values().stream()
                .map(jmdns -> CompletableFuture.runAsync(() -> {
                    try (jmdns) {
                        log.debug(
                            "shutting down JmDNS for {}",
                            beautify(jmdns.getInetAddress())
                        );
                        jmdns.unregisterAllServices();
                        jmdns.removeServiceListener(serviceType, this);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })).toArray(CompletableFuture[]::new)
        ).join();

        addressJmDNSMap.clear();
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        // JmDNS handles service resolving automatically.
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        if (hasProperties(event)) {

            ShipService service = new ShipService(event.getInfo(), fixLinkLocal(event));

            if (service.getSocketAddresses().isEmpty()) {
                log.trace(
                    "SHIP Service does not contain any valid socket addresses. It will be ignored: \n\t{}",
                    service
                );
            }
            else if (reportedServices.contains(service)) {
                log.trace(
                    "SHIP Service was already reported. It will be ignored: \n\t{}",
                    service
                );
            }
            else {
                reportedServices.add(service);

                if (connHandler != null && !isUs(service)) {
                    connHandler.serviceAdded(service);
                    log.info("new mDNS service resolved: {}", service.getInstance());
                }

                log.debug(service.toString());
            }
        }
    }

    private boolean isUs(ShipService service) {
        return ownTxt != null
            && Objects.equals(service.getSki(), ownTxt.getSki())
            && Objects.equals(service.getShipId(), ownTxt.getId());
    }

    private Optional<Inet6Address> fixLinkLocal(ServiceInfo info, JmDNS dns) {

        Optional<Inet6Address> inet6 = Arrays
            .stream(info.getInet6Addresses())
            .findFirst();

        if (inet6.isPresent()
            && inet6.get().isLinkLocalAddress()
            && inet6.get().getScopedInterface() == null
        ) {
            try {
                InetAddress dnsAddress = dns.getInetAddress();

                if (dnsAddress instanceof Inet6Address) {
                    Set<Integer> associatedScopes = addressJmDNSMap
                        .keySet()
                        .stream()
                        .filter(Predicate.isEqual(dnsAddress))
                        .map(Inet6Address.class::cast)
                        .map(Inet6Address::getScopeId)
                        .collect(Collectors.toSet());

                    if (associatedScopes.size() == 1) {
                        return Optional.of(Inet6Address.getByAddress(
                            inet6.get().getHostName(),
                            inet6.get().getAddress(),
                            associatedScopes.stream().findAny().orElseThrow()
                        ));
                    }
                }
                return Optional.empty();
            }
            catch (IOException e) {
                // if we can't fix it, it's useless
                return Optional.empty();
            }
        }
        return inet6;
    }

    private Optional<Inet6Address> fixLinkLocal(ServiceEvent event) {
        return fixLinkLocal(event.getInfo(), event.getDNS());
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        log.debug("service removed: {}", event.getName());
        ShipService service = new ShipService(
            event.getInfo(),
            fixLinkLocal(event)
        );
        reportedServices.remove(service);
        if (connHandler != null && !isUs(service)) {
            connHandler.serviceRemoved(service);
        }
    }

    private static boolean hasProperties(ServiceEvent event) {
        return Collections.list(event.getInfo().getPropertyNames()).size() > 1;
    }

    public void setRegisterFlag(boolean to) {
        if (ownTxt.getRegister() != to) {
            ownTxt.setRegister(to);

            if (boundSocketAddresses != null && !boundSocketAddresses.isEmpty()) {
                unregisterAllServices();
                Set<InetSocketAddress> toRegister = Set.copyOf(
                    boundSocketAddresses
                );
                boundSocketAddresses = Collections.emptySet();
                registerServices(toRegister);
            }
        }
    }

    public void setConnHandler(ConnectionHandler connHandler) {
        this.connHandler = connHandler;
    }
}
