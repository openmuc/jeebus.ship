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

import javax.jmdns.ServiceInfo;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class represents a SHIP mDNS Service Info and provides specialized access to
 * all its associated fields and TXT record values.
 *
 * @see "SHIP:7 Discovery"
 */
public class ShipService implements Comparable<ShipService> {

    private final ServiceInfo info;
    private final Optional<InetSocketAddress> inet6Socket;

    public ShipService(ServiceInfo info, Optional<Inet6Address> inet6Address) {
        this.info = info;
        this.inet6Socket = inet6Address.map(address -> new InetSocketAddress(
            address,
            info.getPort()
        ));
    }

    /**
     * @return the human-readable instance part of the service name such as
     * {@code "Dishwasher ExampleCompany EEB01M3EU"}
     */
    public String getInstance() {
        return info.getName();
    }

    /**
     * @return the complete, qualified service name such as
     * {@code "Dishwasher ExampleCompany EEB01M3EU._ship._tcp.local."}
     */
    public String getFullServiceName() {
        return info.getQualifiedName();
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
        String registerStr = info.getPropertyString("register");
        return Boolean.parseBoolean(registerStr);
    }

    /**
     * @return the SHIP node's brand from the TXT record (optional)
     */
    public String getBrand() {
        return info.getPropertyString("brand");
    }

    /**
     * @return the SHIP node's type from the TXT record (optional)
     */
    public String getType() {
        return info.getPropertyString("type");
    }

    /**
     * @return the SHIP node's model from the TXT record (optional)
     */
    public String getModel() {
        return info.getPropertyString("model");
    }

    /**
     * @return {@link ServiceInfo#getServer()}
     */
    public String getServer() {
        return info.getServer();
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
        return info;
    }

    /**
     * @return all socket addresses for this SHIP service
     */
    public Set<InetSocketAddress> getSocketAddresses() {
        return Stream
            .of(getInet4SocketAddress(), inet6Socket)
            .filter(Optional::isPresent)
            .map(Optional::get)
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
            .stream(info.getInet4Addresses())
            .findFirst()
            .map(inet4Address -> new InetSocketAddress(
                inet4Address,
                info.getPort()
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
        return inet6Socket;
    }

    private String safelyReadRecord(String record) {
        String content = info.getPropertyString(record);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("TXT record '"
                + record
                + "' is not set in service "
                + getInstance());
        }
        return content;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        @SuppressWarnings("QuestionableName")
        ShipService that = (ShipService) other;
        return Objects.equals(this.getSocketAddresses(), that.getSocketAddresses())
            && Objects.equals(this.getShipId(), that.getShipId())
            && Objects.equals(this.getSki(), that.getSki());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.getSocketAddresses(),
            this.getShipId(),
            this.getSki()
        );
    }

    @Override
    public int compareTo(ShipService other) {
        return Integer.compare(Objects.hashCode(this), Objects.hashCode(other));
    }

    @Override
    public String toString() {
        String delimiter = "\n\t";
        return info.getQualifiedName()
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
            .list(info.getPropertyNames())
            .stream()
            .map(prop -> prop + ": " + info.getPropertyString(prop))
            .collect(Collectors.joining(delimiter, delimiter, ""));
    }
}
