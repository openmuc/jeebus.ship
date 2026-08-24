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

import org.bouncycastle.asn1.x500.X500Name;
import org.openmuc.jeebus.ship.api.cert.CertificateStorage;
import org.openmuc.jeebus.ship.api.cert.MemoryCertificateStorage;
import org.openmuc.jeebus.ship.node.KeyManagement;
import org.openmuc.jeebus.ship.node.ShipConfig;
import org.openmuc.jeebus.ship.util.ShipUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Builder class for creating new {@link ShipConfig} objects.
 * <p>
 * This class provides a fluent API for configuring various aspects of a SHIP node,
 * including network settings, security parameters, mDNS service discovery, and
 * device identification. It enforces validation rules and provides sensible defaults
 * for optional parameters.
 * <p>
 * Key configuration areas:
 * <ul>
 *     <li>SHIP node identification (ID, brand, type, model)</li>
 *     <li>Network binding (IPv4/IPv6 addresses and ports)</li>
 *     <li>Security (keystore, certificates, trusted SKIs)</li>
 *     <li>mDNS service discovery (service instance, domain)</li>
 * </ul>
 * <p>
 * Use the {@link #build()} method to create a validated configuration after setting
 * the required parameters.
 */
public final class ConfigBuilder {
    public static final InetSocketAddress INET4_ANY;
    public static final InetSocketAddress INET6_ANY;

    private static final Logger LOG = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass()
    );

    static {
        try {
            INET4_ANY = new InetSocketAddress(
                Inet4Address.getByAddress(new byte[] { 0, 0, 0, 0 }),
                0
            );
            INET6_ANY = new InetSocketAddress(
                InetAddress.getByName("::"),
                0
            );
        }
        catch (UnknownHostException e) {
            // cannot happen
            throw new RuntimeException(e);
        }
    }

    private String id;

    private boolean serverEnabled = true;
    private Set<InetSocketAddress> serverBindAddresses = Set.of(INET4_ANY);
    private boolean autoAcceptEnabled = false;
    private Set<String> trustedSkis = Collections.emptySet();

    private long networkInterfaceScanInterval = 10;
    private long networkInterfaceScanInitialDelay = 5;

    private String mDnsServiceInstance;
    private String mDnsDomain = "local.";
    private String brand = "jEEBus";
    private String type = "default";
    private String model = "default";

    private CertificateStorage certificateStorage = new MemoryCertificateStorage();
    private int certificateValidity = 3650;
    private String certificateDistinguishedName;

    @SuppressWarnings("HardcodedFileSeparator")
    private String wssPath = "/ship/";
    private boolean keepAlive = true;

    public ConfigBuilder() {
    }

    public static ConfigBuilder aShipConfig() {
        return new ConfigBuilder();
    }

    /**
     * The SHIP ID is a globally unique identifier of the SHIP node. It occupies the
     * id field in the mDNS TXT record. The first part of the ID SHOULD be an
     * abbreviation of the manufacturer name. Behind the abbreviation, the
     * manufacturer defines a unique identifier.
     * <p>
     * Example Values: {@code EXAMPLEBRAND-EEB01M3EU-001122334455}
     * <p>
     * SHALL NOT exceed 63 bytes in size.
     *
     * @param id
     *     the SHIP-ID of the new SHIP node
     * @return the updated {@link ConfigBuilder}
     * @see "SHIP:7.3.2 TXT Record"
     */
    public ConfigBuilder withId(String id) {
        validateSizeInBytes("SHIP-ID", id);
        this.id = id;
        return this;
    }

    /**
     * The given socket addresses are used both for binding the SHIP server and SHIP
     * service discovery using mDNS. This includes finding remote SHIP services as
     * well as registering the service of this device.
     * <p>
     * By default, this is set to the ANY adress ({@code 0.0.0.0:0}) to consider all
     * available network interfaces and use ephemeral free ports. {@code [::]:0} does
     * the same. If set to the ANY address, the SHIP node will also scan for new
     * network interfaces during runtime to bind its server to and register new mDNS
     * services. Else it will only consider addresses given here.
     * <p>
     * Support for multiple socket addresses with different port numbers is limited
     * and can lead to wrong mDNS services being registered or discovered. It is
     * recommended to choose one free port number and use that for all sockets.
     *
     * @param serverBindAddresses
     *     Socket addresses to bind the SHIP server to. IPv4 and IPv6 addresses are
     *     supported. Please make sure the given ports are free.
     * @return the updated {@link ConfigBuilder}
     * @see "RFC 2732; RFC 2373"
     */
    public ConfigBuilder withServerBindAddresses(
        Set<InetSocketAddress> serverBindAddresses
    ) {
        if (serverBindAddresses.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                "Set of server bind addresses shall not contain null"
            );
        }
        if (serverBindAddresses.size() > 1
            && findTheAnyAddress(serverBindAddresses).isPresent()
        ) {
            throw new IllegalArgumentException(
                "Combining an ANY address (0.0.0.0 or ::) with a regular IP address"
                    + " is not supported"
            );
        }
        this.serverBindAddresses = serverBindAddresses;
        return this;
    }

    /**
     * This method parses the socket addresses to bind the SHIP server to from the
     * given Strings. IPv6 host addresses must be surrounded by brackets.
     * <p>
     * Example Values: {@code "localhost:0"};
     * {@code "127.0.0.1:8080"};
     * {@code "[1080:0:0:0:8:800:200C:417A]:2001"};
     * {@code "[1080::8:800:200C:417A]:6060"}
     * <p>
     * Please see {@link ConfigBuilder#withServerBindAddresses(Set)} for further
     * important information on socket address configuration.
     *
     * @param serverBindAddresses
     *     String representations of the socket address to bind the SHIP server to
     * @return {@link ConfigBuilder#withServerBindAddresses(Set)}
     * @see "RFC 2732; RFC 2373"
     */
    public ConfigBuilder withServerBindAddresses(String... serverBindAddresses) {
        Set<InetSocketAddress> result = Arrays
            .stream(serverBindAddresses)
            .map(ShipUtilities::safelyParseSocketAddress)
            .collect(Collectors.toSet());

        return withServerBindAddresses(result);
    }

    /**
     * @param serverEnabled
     *     If {@code true} (which is the default), starts a SHIP server with the
     *     new node. According to the SHIP specification, disabling the SHIP server
     *     is only valid in certain scenarios. Please consult the specification to be
     *     sure.
     * @return the updated {@link ConfigBuilder}
     */
    public ConfigBuilder withServerEnabled(boolean serverEnabled) {
        this.serverEnabled = serverEnabled;
        return this;
    }

    /**
     * @param autoAcceptEnabled
     *     If {@code true}, the SHIP server accepts the first incoming connection of
     *     another device within two minutes after starting. The trust is not
     *     persisted.
     *
     * @return the updated {@link ConfigBuilder}
     *
     * @see "SHIP:12.3.1.1 Auto Accept"
     *
     * @deprecated since 3.0.0. Usage of the auto accept mode is discouraged by the
     * EEBus Initiative and most stack implementers. Experience has shown even one
     * device in auto accept mode makes setting up working EEBus networks hard and
     * unreliable. It may be removed in future SHIP specification versions.
     */
    @Deprecated(since = "3.0.0")
    public ConfigBuilder withAutoAcceptEnabled(boolean autoAcceptEnabled) {
        this.autoAcceptEnabled = autoAcceptEnabled;
        return this;
    }

    /**
     * For mDNS services and the SHIP server to react to changes in the available
     * network interfaces jEEBus.SHIP does periodic scans and refreshes its server,
     * services and service listeners accordingly. Here you can configure the
     * interval between these scans in seconds.
     *
     * @param intervalInSeconds
     *     the interval between network interface scans
     * @return the updated {@link ConfigBuilder}
     * @see ConfigBuilder#withNetworkInterfaceScanInitialDelay(long)
     */
    public ConfigBuilder withNetworkInterfaceScanInterval(long intervalInSeconds) {
        this.networkInterfaceScanInterval = intervalInSeconds;
        return this;
    }

    /**
     * For mDNS services and the SHIP server to react to changes in the available
     * network interfaces jEEBus.SHIP does periodic scans and refreshes its server,
     * services and service listeners accordingly. Here you can configure an initial
     * delay before the scanning starts in seconds. Defaults to {@code 5} seconds.
     * This will also delay mDNS scanning as a whole to give the SHIP node some time
     * to initialize before remote SHIP services are reported.
     *
     * @param initialDelayInSeconds
     *     the initial delay before network interface scans
     * @return the updated {@link ConfigBuilder}
     * @see ConfigBuilder#withNetworkInterfaceScanInterval(long)
     */
    public ConfigBuilder withNetworkInterfaceScanInitialDelay(
        long initialDelayInSeconds
    ) {
        this.networkInterfaceScanInitialDelay = initialDelayInSeconds;
        return this;
    }

    /**
     * Service instance label for the mDNS record. It should be user-friendly and may
     * contain the device type, brand and model. It is unique across the network,
     * automatically suffixing a counter on name conflicts.
     * <p>
     * Example Values: {@code Dishwasher ExampleCompany EEB01M3EU}
     * <p>
     * SHALL NOT exceed 64 bytes in size.
     *
     * @param mDnsServiceInstance
     *     label for the service instance of the new SHIP server
     * @return the updated {@link ConfigBuilder}
     * @see "SHIP:7.1 Service Instance"
     */
    public ConfigBuilder withMDnsServiceInstance(String mDnsServiceInstance) {
        validateSizeInBytes("mDNS Service Instance", mDnsServiceInstance);
        this.mDnsServiceInstance = mDnsServiceInstance;
        return this;
    }

    /**
     * @param mDnsDomain
     *     Domain component for mDNS. This SHIP node will register it's mDNS service
     *     in this domain and will only consider other SHIP services within this
     *     domain. Best leave this set to {@code "local."} (which is the
     *     default) unless you really know what you are doing.
     * @return the updated {@link ConfigBuilder}
     * @see "7.2 Service Name"
     */
    public ConfigBuilder withMDnsDomain(String mDnsDomain) {
        this.mDnsDomain = mDnsDomain.replace(".", "") + ".";
        return this;
    }

    /**
     * @param storage
     *     this certificate storage is used to save and load key information.
     *     Defaults to {@link MemoryCertificateStorage}.
     * @return the updated {@link ConfigBuilder}
     */
    public ConfigBuilder withCertificateStorage(CertificateStorage storage) {
        this.certificateStorage = storage;
        return this;
    }

    /**
     * @param certificateValidity
     *     the number of days the certificate is valid for. Defaults to
     *     {@code 3650}.
     * @return the updated {@link ConfigBuilder}
     */
    public ConfigBuilder withCertificateValidity(int certificateValidity) {
        this.certificateValidity = certificateValidity;
        return this;
    }

    /**
     * @param certificateDistinguishedName
     *     the distinguished name in the certificate. Setting the Common Name field
     *     is mandatory (i.e. {@code "CN=myJeebusDevice"}).
     * @return the updated {@link ConfigBuilder}
     * @see "RFC 3280; RFC 4514; X.509"
     */
    public ConfigBuilder withCertificateDistinguishedName(
        String certificateDistinguishedName
    ) {
        try {
            new X500Name(certificateDistinguishedName);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Failure when validating certificate distinguished name: ",
                e
            );
        }
        this.certificateDistinguishedName = certificateDistinguishedName;
        return this;
    }

    /**
     * @param wssPath
     *     Websocket path of the SHIP node. Occupies the path field in the mDNS TXT
     *     record. Best leave this set to the default value
     *     {@code "/ship/"} unless you really know what you are doing.
     * @return the updated {@link ConfigBuilder}
     * @see "7.3.2 TXT Record"
     */
    public ConfigBuilder withWssPath(String wssPath) {
        if (wssPath == null || wssPath.isEmpty()) {
            throw new IllegalArgumentException("wssPath must not be empty");
        }

        this.wssPath = wssPath;
        return this;
    }

    /**
     * @param keepAlive
     *     If {@code true}, Enables Netty socket keep-alive mechanism.
     * @return the updated {@link ConfigBuilder}
     * @see "SHIP:10.4 Connection Keepalive; RFC 1122:4.2.3.6 TCP Keep-Alives"
     */
    public ConfigBuilder withKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    /**
     * @param brand
     *     The brand of the device. It is announced via the mDNS TXT record. Defaults
     *     to {@code "jEEBus"}.
     * @return the updated {@link ConfigBuilder}
     */
    public ConfigBuilder withBrand(String brand) {
        this.brand = brand;
        return this;
    }

    /**
     * @param type
     *     The type of the device. It is announced via the mDNS TXT record. Example
     *     value: {@code Dishwasher} Defaults to
     *     {@code "default"}.
     * @return the updated {@link ConfigBuilder}
     */
    public ConfigBuilder withType(String type) {
        this.type = type;
        return this;
    }

    /**
     * @param model
     *     The model of the device. It is announced via the mDNS TXT record. Example
     *     value: {@code EEB01M3EU} Defaults to
     *     {@code "default"}.
     * @return the updated {@link ConfigBuilder}
     */
    public ConfigBuilder withModel(String model) {
        this.model = model;
        return this;
    }

    /**
     * @param trustedSkis
     *     a set of Subject Key Identifiers (SKIs) to establish pre-trust with other
     *     SHIP nodes. Each SKI is given as a String of 40 hexadecimal digits.
     * @return the updated {@link ConfigBuilder}
     * @see "SHIP:12.2 SHIP Node Specific Public Key"
     */
    public ConfigBuilder withTrustedSkis(Set<String> trustedSkis) {
        this.trustedSkis = trustedSkis;
        return this;
    }

    /**
     * @param trustedSkis
     *     the SKIs to establish pre-trust.
     * @return {@link ConfigBuilder#withTrustedSkis(Set)}
     */
    public ConfigBuilder withTrustedSkis(String... trustedSkis) {
        return withTrustedSkis(Set.of(trustedSkis));
    }

    /**
     * @return a copy of this object.
     */
    public ConfigBuilder but() {
        return aShipConfig()
            .withId(id)
            .withServerEnabled(serverEnabled)
            .withServerBindAddresses(serverBindAddresses)
            .withAutoAcceptEnabled(autoAcceptEnabled)
            .withTrustedSkis(trustedSkis)
            .withMDnsServiceInstance(mDnsServiceInstance)
            .withMDnsDomain(mDnsDomain)
            .withBrand(brand)
            .withType(type)
            .withModel(model)
            .withCertificateStorage(certificateStorage)
            .withCertificateValidity(certificateValidity)
            .withCertificateDistinguishedName(certificateDistinguishedName)
            .withWssPath(wssPath)
            .withKeepAlive(keepAlive);
    }

    /**
     * Validates and builds the new {@link ShipConfig}.
     * <p>
     * The following methods must be called before this:
     * <ul>
     *     <li>{@link ConfigBuilder#withId(String)}</li>
     *     <li>{@link ConfigBuilder#withMDnsServiceInstance(String)}</li>
     *     <li>{@link ConfigBuilder#withCertificateDistinguishedName(String)}</li>
     * </ul>
     *
     * @return the new {@link ShipConfig}
     */
    public ShipConfig build() {
        if (Stream.of(
            id,
            mDnsServiceInstance,
            certificateDistinguishedName
        ).anyMatch(Objects::isNull)
            || (serverBindAddresses == null || serverBindAddresses.isEmpty()
            && serverEnabled)
        ) {
            throw new IllegalStateException(
                "id, serverBindAddresses, mDnsServiceInstance and "
                    + "certificateDistinguishedName must be set"
            );
        }
        Optional<InetSocketAddress> theAnyAddress = findTheAnyAddress(
            serverBindAddresses
        );
        if (theAnyAddress.isPresent()) {
            LOG.info(
                "SHIP server bind address is set to the ANY address. Using all"
                    + " available network interfaces for server binding"
                    + " and mDNS Service Discovery."
            );
        }
        Set<String> invalidSkis = trustedSkis
            .stream()
            .filter(Predicate.not(KeyManagement::isValidSki))
            .collect(Collectors.toSet());
        if (!invalidSkis.isEmpty()) {
            throw new IllegalStateException(
                "There were invalid SKIs: " + invalidSkis
            );
        }

        return new ShipConfig(
            id,
            serverEnabled,
            // if there is an ANY address, disregard other addresses
            theAnyAddress.map(Set::of).orElse(serverBindAddresses),
            theAnyAddress.isPresent(),
            autoAcceptEnabled,
            trustedSkis,
            networkInterfaceScanInitialDelay,
            networkInterfaceScanInterval,
            mDnsServiceInstance,
            mDnsDomain,
            brand,
            type,
            model,
            certificateStorage,
            certificateValidity,
            certificateDistinguishedName,
            wssPath,
            keepAlive
        );
    }

    private static void validateSizeInBytes(String field, String value) {
        if (value.getBytes(UTF_8).length > 63) {
            throw new IllegalArgumentException(field
                + " '"
                + value
                + "' SHALL NOT exceed 63 bytes");
        }
    }

    private static Optional<InetSocketAddress> findTheAnyAddress(
        Set<InetSocketAddress> serverBindAddresses
    ) {
        return serverBindAddresses.stream()
            .filter(socket ->
                INET4_ANY.getAddress().equals(socket.getAddress())
                || INET6_ANY.getAddress().equals(socket.getAddress()))
            .findAny();
    }

}
