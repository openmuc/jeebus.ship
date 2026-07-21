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

import javax.annotation.Nonnull;
import javax.jmdns.ServiceInfo;
import java.net.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class represents a SHIP mDNS Service Info and provides specialized access to
 * all its associated fields and TXT record values.
 *
 * @see "SHIP:7 Discovery"
 */
public class ShipService {

    public static final String WSS_PREFIX = "wss://";
    private final ServiceInfo serviceInfo;

    public ShipService(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    /**
     * @return the human-readable instance part of the service name such as
     * {@code "Dishwasher ExampleCompany EEB01M3EU"}
     */
    public String getInstance() {
        return serviceInfo.getName();
    }

    /**
     * @return the complete, qualified service name such as
     * {@code "Dishwasher ExampleCompany EEB01M3EU._ship._tcp.local."}
     */
    public String getFullServiceName() {
        return serviceInfo.getQualifiedName();
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
     * @return the service's path from the TXT record
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
        String registerStr = serviceInfo.getPropertyString("register");
        return Boolean.parseBoolean(registerStr);
    }

    /**
     * @return the SHIP node's brand from the TXT record (optional)
     */
    public String getBrand() {
        return serviceInfo.getPropertyString("brand");
    }

    /**
     * @return the SHIP node's type from the TXT record (optional)
     */
    public String getType() {
        return serviceInfo.getPropertyString("type");
    }

    /**
     * @return the SHIP node's model from the TXT record (optional)
     */
    public String getModel() {
        return serviceInfo.getPropertyString("model");
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
        return serviceInfo;
    }

    /**
     * @return all socket addresses for this SHIP service
     */
    public Set<InetSocketAddress> getSocketAddresses() {
        return Arrays
            .stream(serviceInfo.getInetAddresses())
            .map(address -> new InetSocketAddress(
                address.getHostName(),
                serviceInfo.getPort()
            ))
            .collect(Collectors.toSet());
    }

    /**
     * @return the IPv4 socket address the SHIP server is bound to or {@code null} if
     * the service info does not contain a IPv4 address.
     * @implNote Apparently, mDNS services may contain multiple IPv4 addresses for a
     * single service in multi-homed environments (see
     * {@link ServiceInfo#getInet4Addresses()}). However, such setups are out of
     * scope for SHIP, so we simply return the first identified address.
     */
    public InetSocketAddress getInet4SocketAddress() {
        Optional<Inet4Address> first = Arrays
            .stream(serviceInfo.getInet4Addresses())
            .findFirst();

        return first
            .map(inet4Address -> new InetSocketAddress(
                inet4Address,
                serviceInfo.getPort()
            ))
            .orElse(null);
    }

    /**
     * @return the IPv6 socket address the SHIP server is bound to or {@code null} if
     * the service info does not contain a IPv6 address.
     * @implNote Apparently, mDNS services may contain multiple IPv6 addresses for a
     * single service in multi-homed environments (see
     * {@link ServiceInfo#getInet4Addresses()}). However, such setups are out of
     * scope for SHIP, so we simply return the first identified address.
     */
    public InetSocketAddress getInet6SocketAddress() {
        Optional<Inet6Address> first = Arrays
            .stream(serviceInfo.getInet6Addresses())
            .findFirst();

        return first
            .map(inet6Address -> new InetSocketAddress(
                inet6Address,
                serviceInfo.getPort()
            ))
            .orElse(null);
    }

    /**
     * @param socketAddress
     *     the socket address to format as a valid SHIP server URI.
     * @return a valid URI for the SHIP server from the given socket address
     * @throws URISyntaxException
     *     if a valid URI cannot be constructed from the information in this SHIP
     *     service and the given socket address.
     */
    public URI toUri(InetSocketAddress socketAddress) throws URISyntaxException {
        String path = fixPath();

        return new URI(WSS_PREFIX + socketAddress + path);
    }

    /**
     * simply calls {@link ShipService#toUri(InetSocketAddress)} with
     * {@link ShipService#getInet4SocketAddress()}
     */
    public URI getInet4Uri() throws URISyntaxException {
        return toUri(getInet4SocketAddress());
    }

    /**
     * simply calls {@link ShipService#toUri(InetSocketAddress)} with
     * {@link ShipService#getInet6SocketAddress()}
     */
    public URI getInet6Uri() throws URISyntaxException {
        return toUri(getInet6SocketAddress());
    }

    private String safelyReadRecord(String record) {
        String content = serviceInfo.getPropertyString(record);
        if (content == null) {
            throw new IllegalStateException("TXT record '"
                + record
                + "' is null for device "
                + getInstance());
        }
        return content;
    }

    @Nonnull
    private String fixPath() {
        String result = getPath();
        if (!result.startsWith("/")) {
            result = "/" + result;
        }
        if (!result.endsWith("/")) {
            result += "/";
        }
        return result;
    }

    @Override
    public String toString() {
        String delimiter = "\n\t";
        return serviceInfo.getQualifiedName()
            + ":"
            + delimiter
            + "addesses: "
            + getSocketAddresses()
            .stream()
            .map(ShipUtilities::beautify)
            .collect(Collectors.joining("; "))
            + Collections
            .list(serviceInfo.getPropertyNames())
            .stream()
            .map(prop -> prop + ": " + serviceInfo.getPropertyString(prop))
            .collect(Collectors.joining(delimiter, delimiter, ""));
    }
}
