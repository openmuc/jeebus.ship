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

import org.bouncycastle.util.Arrays;
import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServiceRegistry implements ServiceListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The JmDNS instances for this service registry. There will be one such instance
     * for every network interface where the SHIP node publishes its service.
     */
    private final Set<JmDNS> jmDNSSet;

    private final String serviceType;

    private final ConnectionHandler connHandler;

    private final Map<String, String> ipServiceNameMap = new HashMap<>();

    private final Set<String> loggedIds = new HashSet<>();

    private static final InetAddress IPv4_ZERO;
    static {
        try {
            IPv4_ZERO = Inet4Address.getByAddress(new byte[]{0,0,0,0});
        }
        catch (UnknownHostException e) {
            // cannot happen
            throw new RuntimeException(e);
        }
    }

    /**
     * creates a JmDNS instance and a service listener
     *
     * @param addresses
     *     the addresses to bind JmDNS instances to
     * @param hostname
     *     mDNS name for the service host
     * @param serviceType
     *     the service type to listen for
     * @param connHandler
     *     connection handler to handle all interactions with the SHIP peer
     */
    public ServiceRegistry(
        Collection<InetAddress> addresses,
        String hostname,
        String serviceType,
        ConnectionHandler connHandler
    ) {
        // set discovery TTL to 2 minutes as per SHIP specification
        System.setProperty("net.dns.ttl", "120");

        this.serviceType = serviceType;

        this.connHandler = connHandler;

        Stream<InetAddress> actualAddresses = gatherAddresses(addresses);
        jmDNSSet = actualAddresses.map(addr -> {
            try {
                JmDNS jmdns = JmDNS.create(addr, hostname);
                jmdns.addServiceListener(serviceType, this);
                return jmdns;
            }
            catch (IOException e) {
                log.error("exception while initiating mDNS service: ", e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * creates a JmDNS instance and a service listener
     *
     * @param address
     *     the address to bind JmDNS instance to
     * @param hostname
     *     mDNS name for the service host
     * @param serviceType
     *     the service type to listen for
     * @param connHandler
     *     connection handler to handle all interactions with the SHIP peer
     */
    public ServiceRegistry(
        InetAddress address,
        String hostname,
        String serviceType,
        ConnectionHandler connHandler
    ) {
        this(Collections.singleton(address), hostname, serviceType, connHandler);
    }

    /**
     * creates service info, typically used to register a service
     *
     * @param serviceName
     *     service name or service type, example: "_ship._tcp.local."
     * @param serviceInstance
     *     service instance, example: "Dishwasher ExampleCompany EEB01M3EU"
     * @param port
     *     the port the service is bound to
     * @param txt
     *     a description for the service
     * @return ServiceInfo object which holds the compressed information passed in
     * parameters
     */
    public static ServiceInfo createServiceInfo(
        String serviceName,
        String serviceInstance,
        int port,
        TxtRecord txt
    ) {
        // TODO use a map to write all name/value pairs instead of a single string
        ServiceInfo serviceInfo = ServiceInfo.create(
            serviceName,
            serviceInstance,
            port,
            0,
            0,
            txt.getTxtRecordProps()
        );

        if (serviceInfo.getTextBytes().length > 400) {
            throw new IllegalArgumentException(
                "According to SHIP:7.3.2, the TXT record SHALL NOT exceed 400 "
                    + "bytes in length.");
        }

        return serviceInfo;
    }

    /**
     * lists all identified services found across all active network interfaces.
     *
     * @return identified services
     */
    public ServiceInfo[] listServices() {
        return jmDNSSet
            .stream()
            .flatMap(j -> java.util.Arrays.stream(j.list(serviceType)))
            .collect(Collectors.toSet())  // remove duplicates
            .toArray(ServiceInfo[]::new);
    }

    /**
     * register a service in own JmDNS instance, other active JmDNS listening for the
     * same service type will identify this service once registered
     *
     * @param serviceInfo
     *     the ServiceInfo object to register
     * @throws IOException
     *     if there is an error in the underlying protocol, such as a TCP error
     */
    public void registerService(ServiceInfo serviceInfo) throws IOException {
        // validate service instance/name here
        for (JmDNS jmdns: jmDNSSet) {
            jmdns.registerService(serviceInfo.clone());
        }
    }

    public void changeService(ServiceInfo oldInfo, ServiceInfo newInfo) throws
        IOException {
        unregisterService(oldInfo);
        registerService(newInfo);
    }

    /**
     * unregister a service in own JmDNS instance
     *
     * @param serviceInfo
     *     the ServiceInfo object to unregister
     */
    public void unregisterService(ServiceInfo serviceInfo) {
        for (JmDNS jmdns : jmDNSSet) {
            jmdns.unregisterService(serviceInfo);
        }
    }

    /**
     * unregisters all services in own JmDNS instance, removed service listener and
     * closes JmDNS instance
     *
     * @throws IOException
     *     if an I/O error occurs
     */
    public void shutdown() throws IOException {
        log.info("shutting down mDNS service registry");
        for (JmDNS jmdns : jmDNSSet) {
            jmdns.unregisterAllServices();
            jmdns.removeServiceListener(serviceType, this);
            jmdns.close();
        }
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
            + "addess: " + getIpAndPort(what)
            + Collections.list(what.getPropertyNames())
            .stream()
            .map(prop -> prop + ": " + what.getPropertyString(prop))
            .collect(Collectors.joining(delimiter, delimiter, ""));
    }

    private static boolean hasProperties(ServiceEvent event) {
        return event.getInfo().getPropertyNames().hasMoreElements();
    }

    private static String getIpAndPort(ServiceInfo info) {
        int port = info.getPort();
        if (Arrays.isNullOrEmpty(info.getInet4Addresses())) {
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

    private Stream<InetAddress> gatherAddresses(Collection<InetAddress> rawAddresses) {
        if (rawAddresses.size() != 1) return rawAddresses.stream();
        // todo figure out the mess around null addresses
        Optional<InetAddress> address0 = rawAddresses.stream().map(Optional::ofNullable).findAny().orElseThrow();
        if (address0.isEmpty()) return Stream.of((InetAddress) null);
        if (!IPv4_ZERO.equals(address0.get())) {
            return Stream.of(address0.get());
        }
        else {
            try {
                return NetworkInterface
                    .networkInterfaces()
                    .filter(iface -> {
                        try {
                            return iface.supportsMulticast();
                        }
                        catch (SocketException e) {
                            log.error(
                                "network interface doesn't know if it supports"
                                    + " multicast??");
                            return false;
                        }
                    })
                    .flatMap(NetworkInterface::inetAddresses);
            } catch (IOException e) {
                log.error("could not enumerate network interfaces: ", e);
                return Stream.of();
            }
        }
    }
}
