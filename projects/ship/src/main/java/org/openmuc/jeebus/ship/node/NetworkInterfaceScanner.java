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

import org.openmuc.jeebus.ship.node.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.openmuc.jeebus.ship.util.ShipUtilities.SCOPED_ADDRESS_ORDER;

public class NetworkInterfaceScanner implements AutoCloseable {

    private final ServiceRegistry serviceRegistry;
    private final ShipNodeImpl node;
    private final ShipConfig config;

    private final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOG = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass()
    );
    private Integer globalServerPort;
    private Set<InetAddress> targetAddresses;
    private Set<InetAddress> previousAddresses;

    public NetworkInterfaceScanner(
        ServiceRegistry serviceRegistry,
        ShipNodeImpl node,
        ShipConfig config
    ) {
        this.serviceRegistry = serviceRegistry;
        this.node = node;
        this.config = config;

        if (config.getAnyAddressEnabled()
            && config.getServerEnabled()
        ) {
            globalServerPort = node
                .getServer()
                .orElseThrow()
                .getBoundSocketAddresses()
                .stream()
                .findAny()
                .orElseThrow()
                .getPort();
        }
        else {
            targetAddresses = config
                .getServerBindAddresses()
                .stream()
                .map(InetSocketAddress::getAddress)
                .collect(toScopedAddressTreeSet());
        }

        executor.scheduleWithFixedDelay(
            this::scanInterfaces,
            // Let's delay scanning and mDNS to make sure SHIP is properly
            // initialized first
            config.getNetworkInterfaceScanInitialDelay(),
            config.getNetworkInterfaceScanInterval(),
            TimeUnit.SECONDS
        );
    }

    @Nonnull
    private static Collector<InetAddress, ?, TreeSet<InetAddress>> toScopedAddressTreeSet() {
        return Collectors.toCollection(() -> new TreeSet<>(SCOPED_ADDRESS_ORDER));
    }

    private void scanInterfaces() {
        try {
            Set<InetAddress> liveAddresses = currentInterfaceAddresses();

            if (!Objects.equals(previousAddresses, liveAddresses)) {
                if (config.getAnyAddressEnabled()) {
                    serviceRegistry.updateListeners(liveAddresses);

                    if (config.getServerEnabled()) {
                        // The SHIP server handles interface changes automatically
                        // when bound to the ANY address. JmDNS does not.
                        serviceRegistry.registerServices(liveAddresses
                            .stream()
                            .map(a -> new InetSocketAddress(a, globalServerPort))
                            .collect(Collectors.toSet()));
                    }
                }
                else {
                    if (config.getServerEnabled()) {

                        // Let's not rebind the server on the first scan
                        if (previousAddresses != null) {
                            node.getServer().orElseThrow().bindTo(
                                config.getServerBindAddresses()
                                    .stream()
                                    .filter(socket -> liveAddresses.contains(socket.getAddress()))
                                    .collect(Collectors.toSet())
                            );
                        }

                        serviceRegistry.registerServices(node
                            .getServer()
                            .orElseThrow()
                            .getBoundSocketAddresses());
                    }

                    serviceRegistry.updateListeners(liveAddresses
                        .stream()
                        .filter(targetAddresses::contains)
                        .collect(toScopedAddressTreeSet()));
                }
                previousAddresses = liveAddresses;
            }
        }
        catch (Exception e) {
            LOG.warn("Error while scanning for new network interfaces", e);
        }
    }

    private Set<InetAddress> currentInterfaceAddresses() throws SocketException {
        return Collections.list(NetworkInterface.getNetworkInterfaces())
            .stream()
            .filter(NetworkInterfaceScanner::checkSuitable)
            .map(NetworkInterface::getInetAddresses)
            .map(Collections::list)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private static boolean checkSuitable(NetworkInterface adapter) {
        try {
            return adapter.isUp() && adapter.supportsMulticast();
        }
        catch (SocketException e) {
            // If we can't even check the interface, it might as well be down...
            return false;
        }
    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
    }
}
