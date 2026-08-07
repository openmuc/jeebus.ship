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

import org.openmuc.jeebus.ship.util.ShipUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents a SHIP mDNS Service Info and provides specialized access to
 * all its associated fields and TXT record values.
 *
 * @see "SHIP:7 Discovery"
 */
public class ShipService {

    private static final Logger LOG = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass()
    );

    private final ServiceEvent event;

    public ShipService(ServiceEvent event) {
        this.event = event;
    }

    /**
     * @return the human-readable instance part of the service name such as
     * {@code "Dishwasher ExampleCompany EEB01M3EU"}
     */
    public String getInstance() {
        return event.getInfo().getName();
    }

    /**
     * @return the complete, qualified service name such as
     * {@code "Dishwasher ExampleCompany EEB01M3EU._ship._tcp.local."}
     */
    public String getFullServiceName() {
        return event.getInfo().getQualifiedName();
    }

    /**
     * @return the SHIP ID from the TXT records
     * @throws IllegalStateException
     *     if the record is empty. It may be technically allowed in SHIP 1.0.1 but in
     *     prectice, SHIP IDs are mandatory. Thus, we do not support SHIP connections
     *     with nodes that have no SHIP ID.
     */
    public String getShipId() {
        return safelyReadRecord("id");
    }

    /**
     * @return the service's WSS path from the TXT record
     * @throws IllegalStateException
     *     if the record is empty, as that is not compliant with the SHIP
     *     specification
     */
    public String getPath() {
        return safelyReadRecord("path");
    }

    /**
     * @return the SHIP node's SKI (Subject Key Identifier) from the TXT record
     * @throws IllegalStateException
     *     if the record is empty, as that is not compliant with the SHIP
     *     specification
     */
    public String getSki() {
        return safelyReadRecord("ski");
    }

    /**
     * @return {@code true} if auto-accept is active in the SHIP node. In mDNS, this
     * is visible with the {@code register} flag.
     */
    public boolean getAutoAccept() {
        String registerStr = event.getInfo().getPropertyString("register");
        return Boolean.parseBoolean(registerStr);
    }

    /**
     * @return the SHIP node's brand from the TXT record (optional)
     */
    public String getBrand() {
        return event.getInfo().getPropertyString("brand");
    }

    /**
     * @return the SHIP node's type from the TXT record (optional)
     */
    public String getType() {
        return event.getInfo().getPropertyString("type");
    }

    /**
     * @return the SHIP node's model from the TXT record (optional)
     */
    public String getModel() {
        return event.getInfo().getPropertyString("model");
    }

    /**
     * @return {@link ServiceInfo#getServer()}
     */
    public String getServer() {
        return event.getInfo().getServer();
    }

    /**
     * @return the TXT record version
     * @throws IllegalStateException
     *     if the record is empty, as that is not compliant with the SHIP
     *     specification
     */
    public int getTxtVersion() {
        return Integer.parseInt(safelyReadRecord("txtvers"));
    }

    /**
     * @return the underlying ServiceInfo object
     */
    public ServiceInfo getServiceInfo() {
        return event.getInfo();
    }

    /**
     * @return all socket addresses for this SHIP service
     */
    public Set<InetSocketAddress> getSocketAddresses() {
        return Arrays
            .stream(event.getInfo().getInetAddresses())
            .map(this::fixLinkLocal)
            .filter(Objects::nonNull)
            .map(address -> new InetSocketAddress(
                address,
                event.getInfo().getPort()
            ))
            .collect(Collectors.toSet());
    }

    /**
     * @return an optional containing the IPv4 socket address if the SHIP service
     * contains one.
     * @implNote Apparently, mDNS services may contain multiple IPv4 addresses for a
     * single service in multi-homed environments (see
     * {@link ServiceInfo#getInet4Addresses()}). However, such setups are out of
     * scope for SHIP, so we simply return the first identified address.
     */
    public Optional<InetSocketAddress> getInet4SocketAddress() {
        return Arrays
            .stream(event.getInfo().getInet4Addresses())
            .findFirst()
            .map(inet4Address -> new InetSocketAddress(
                inet4Address,
                event.getInfo().getPort()
            ));
    }

    /**
     * @return an optional containing the IPv6 socket address if the SHIP service
     * contains one.
     * @implNote Apparently, mDNS services may contain multiple IPv6 addresses for a
     * single service in multi-homed environments (see
     * {@link ServiceInfo#getInet4Addresses()}). However, such setups are out of
     * scope for SHIP, so we simply return the first identified address.
     */
    public Optional<InetSocketAddress> getInet6SocketAddress() {
        // noinspection ConstantValue
        return Arrays
            .stream(event.getInfo().getInet6Addresses())
            .findFirst()
            .map(this::fixLinkLocal)
            .filter(Objects::nonNull)
            .map(inet6Address -> new InetSocketAddress(
                inet6Address,
                event.getInfo().getPort()
            ));
    }

    private InetAddress fixLinkLocal(InetAddress address) {
        if (address instanceof Inet6Address
            && address.isLinkLocalAddress()
            && ((Inet6Address) address).getScopedInterface() == null
        ) {
            try {
                return Inet6Address.getByAddress(
                    event.getInfo().getInet6Addresses()[0].getHostName(),
                    event.getInfo().getInet6Addresses()[0].getAddress(),
                    ((Inet6Address) event.getDNS().getInetAddress()).getScopeId()
                );
            }
            catch (IOException | ClassCastException e) {
                // An unscoped link-local IPv6 address is useless.
                // Let's not expose it to users.
                return null;
            }
        }
        return address;
    }

    private String safelyReadRecord(String record) {
        String content = event.getInfo().getPropertyString(record);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("TXT record '"
                + record
                + "' is not set in service "
                + getInstance());
        }
        return content;
    }

    @Override
    public String toString() {
        String delimiter = "\n\t";
        return event.getInfo().getQualifiedName()
            + ":"
            + delimiter
            + "addresses: "
            + getSocketAddresses()
            .stream()
            .map(ShipUtilities::beautify)
            .collect(Collectors.joining("; "))
            + delimiter
            + "server: "
            + getServer()
            + Collections
            .list(event.getInfo().getPropertyNames())
            .stream()
            .map(prop -> prop + ": " + event.getInfo().getPropertyString(prop))
            .collect(Collectors.joining(delimiter, delimiter, ""));
    }
}
