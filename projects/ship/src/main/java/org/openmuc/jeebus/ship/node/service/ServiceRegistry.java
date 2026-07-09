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
import org.openmuc.jeebus.ship.node.ShipConfig;
import org.openmuc.jeebus.ship.util.ShipUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.bouncycastle.util.Arrays.isNullOrEmpty;

public class ServiceRegistry implements ServiceListener, AutoCloseable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The JmDNS instances for this service registry. There will be one such instance
     * for every network interface where the SHIP node publishes its service.
     */
    private final Set<JmDNS> jmDnsSet;

    private final String serviceType;

    private final ConnectionHandler connHandler;

    private final Map<String, String> ipServiceNameMap = new HashMap<>();

    private final Set<String> loggedIds = new HashSet<>();

    private final String hostname;

    private final ShipConfig config;

    private TxtRecord ownTxt;
    private Set<InetSocketAddress> boundSocketAddresses;

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

        jmDnsSet = config.getServerBindAddresses()
            .stream()
            .map(InetSocketAddress::getAddress)
            .map(this::safelyInitiateService)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private JmDNS safelyInitiateService(InetAddress address) {
        try {
            JmDNS jmdns = JmDNS.create(address, hostname);
            jmdns.addServiceListener(serviceType, this);
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
        return jmDnsSet
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
        Optional<JmDNS> boundJmDns = jmDnsSet.stream()
            .filter(jmDNS -> Objects.equals(
                safelyGetInetAddress(jmDNS),
                socketAddress.getAddress()
            ))
            .findAny();

        if (boundJmDns.isPresent()) {
            try {
                boundJmDns.get().registerService(
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
        else {
            return null;
        }
    }

    private static InetAddress safelyGetInetAddress(JmDNS jmDNS) {
        try {
            return jmDNS.getInetAddress();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void unregisterAllServices() {
        for (JmDNS jmdns : jmDnsSet) {
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
     * unregisters all services in own JmDNS instance, removed service listener and
     * closes JmDNS instance
     *
     * @throws IOException
     *     if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        log.info("shutting down mDNS service registry");
        CompletableFuture.allOf(
            jmDnsSet.stream()
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
            connHandler.serviceRemoved(ipServiceNameMap.get(event.getName()));
            ipServiceNameMap.remove(event.getName());
        }
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        if (hasProperties(event)) {
            String id = event.getInfo().getPropertyString("id");

            if (loggedIds.contains(id)) {
                log.trace(
                    "mdns service resolved again: {}",
                    formatServiceInfo(event.getInfo())
                );
            }
            else {
                loggedIds.add(id);

                log.info("new mDNS service resolved: {}", event.getName());

                log.debug(formatServiceInfo(event.getInfo()));
            }

            String ipAddr = getIpAndPort(event.getInfo());
            if (!ipServiceNameMap.containsKey(event.getName())
                || !ipServiceNameMap.containsValue(ipAddr)) {
                if (connHandler != null) {
                    String ski = event.getInfo().getPropertyString("ski");
                    if (ski != null) {
                        ipServiceNameMap.put(event.getName(), ipAddr);
                        connHandler.serviceAdded(ipAddr, ski);
                    }
                }
            }
        }
    }

    public static String formatServiceInfo(ServiceInfo what) {
        String delimiter = "\n\t";
        return what.getQualifiedName() + ":" + delimiter
            + "addesses: " + getSocketAddresses(what)
                .stream()
                .map(ShipUtilities::beautify)
                .collect(Collectors.joining("; "))
            + Collections.list(what.getPropertyNames())
            .stream()
            .map(prop -> prop + ": " + what.getPropertyString(prop))
            .collect(Collectors.joining(delimiter, delimiter, ""));
    }

    private static Set<InetSocketAddress> getSocketAddresses(ServiceInfo what) {
        return Arrays
            .stream(what.getInetAddresses())
            .map(address -> new InetSocketAddress(
                address.getHostName(),
                what.getPort()
            ))
            .collect(Collectors.toSet());
    }

    private static boolean hasProperties(ServiceEvent event) {
        return event.getInfo().getPropertyNames().hasMoreElements();
    }

    private static String getIpAndPort(ServiceInfo info) {
        int port = info.getPort();
        if (isNullOrEmpty(info.getInet4Addresses())) {
            return String.format(
                "[%s]:%s",
                info.getInet6Addresses()[0].getHostAddress(),
                port
            );
        }
        else {
            return String.format(
                "%s:%s",
                info.getInet4Addresses()[0].getHostAddress(),
                port
            );
        }
    }
}
