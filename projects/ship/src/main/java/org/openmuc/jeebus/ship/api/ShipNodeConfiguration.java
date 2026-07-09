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

import org.openmuc.jeebus.ship.api.cert.CertificateStorage;
import org.openmuc.jeebus.ship.api.cert.KeyStoreCertificateStorage;
import org.openmuc.jeebus.ship.api.cert.MemoryCertificateStorage;
import org.openmuc.jeebus.ship.node.ShipNodeImpl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * big POJO that holds all relevant library configuration
 *
 * @deprecated since 2.3.0 and scheduled for removal in 3.0.0.
 * This will be replaced by a new Class called {@code ConfigBuilder}.
 */
@Deprecated(since = "2.3.0", forRemoval = true)
public class ShipNodeConfiguration {

    private InetAddress ipAddress;
    private Set<InetAddress> ipAddresses;
    private int port;

    private boolean clientOnly;

    private String wssPath;

    private boolean keepAlive;

    private String serviceId;

    private String serviceDomain;

    private String serviceInstance;

    private CertificateStorage certificateStorage;

    private String distinguishedName;

    private int certificateValidityInDays;

    /**
     * Wrapper class for parameters for initial ship node configuration. If a
     * certificate should be loaded or stored, use the other constructor instead
     *
     * @param ipAddresses
     *     ip addresses of network interfaces to bind this Ship node to, example:
     *     ["192.168.1.2" /* ethernet * /, "::1" /* loopback * /]
     * @param port
     *     port for initial server
     * @param wssPath
     *     wss path for initial server, example: "/ship/"
     * @param keepAlive
     *     indicates if keepAlive packets are allowed
     * @param serviceId
     *     host name for JmDNS instance (service discovery), example:
     *     "EXAMPLEBRAND-EEB01M3EU-001122334455"
     * @param serviceDomain
     *     which domain for service types to listen to, example: "local"
     * @param serviceInstance
     *     service instance label for initial server, example: "Dishwasher
     *     ExampleCompany EEB01M3EU"
     * @param certificateStorage
     *     certificate storage used to load and store the key pair
     * @param distinguishedName
     *     X.509 Distinguished Name, eg "CN=Test, L=London, C=GB". For IoT devices,
     *     usually the DeviceID
     * @param certificateValidityInDays
     *     how many days the certificate should be valid for
     */
    public ShipNodeConfiguration(
        Set<String> ipAddresses,
        int port,
        String wssPath,
        boolean keepAlive,
        String serviceId,
        String serviceDomain,
        String serviceInstance,
        CertificateStorage certificateStorage,
        String distinguishedName,
        int certificateValidityInDays
    ) {
        this(
            ipAddresses,
            port,
            wssPath,
            keepAlive,
            serviceId,
            serviceDomain,
            serviceInstance,
            distinguishedName,
            certificateValidityInDays
        );
        this.certificateStorage = certificateStorage;
    }

    /**
     * Wrapper class for parameters for initial ship node configuration. If a
     * certificate should be loaded or stored, use the other constructor instead
     *
     * @param ipAddresses
     *     ip addresses of network interfaces to bind this Ship node to, example:
     *     ["192.168.1.2" /* ethernet * /, "::1" /* loopback * /]
     * @param port
     *     port for initial server
     * @param wssPath
     *     wss path for initial server, example: "/ship/"
     * @param keepAlive
     *     indicates if keepAlive packets are allowed
     * @param serviceId
     *     host name for JmDNS instance (service discovery), example:
     *     "EXAMPLEBRAND-EEB01M3EU-001122334455"
     * @param serviceDomain
     *     which domain for service types to listen to, example: "local"
     * @param serviceInstance
     *     service instance label for initial server, example: "Dishwasher
     *     ExampleCompany EEB01M3EU"
     * @param alias
     *     the alias for the key pair that should be loaded or created
     * @param keyStorePassphrase
     *     passphrase for the whole key store, see KeyManagement class
     * @param keyPairPassphrase
     *     passphrase for the key pair to be generated
     * @param distinguishedName
     *     X.509 Distinguished Name, eg "CN=Test, L=London, C=GB". For IoT devices,
     *     usually the DeviceID
     * @param certificateValidityInDays
     *     how many days the certificate should be valid for
     */
    public ShipNodeConfiguration(
        Set<String> ipAddresses,
        int port,
        String wssPath,
        boolean keepAlive,
        String serviceId,
        String serviceDomain,
        String serviceInstance,
        String alias,
        char[] keyStorePassphrase,
        char[] keyPairPassphrase,
        String distinguishedName,
        int certificateValidityInDays
    ) {
        this(
            ipAddresses,
            port,
            wssPath,
            keepAlive,
            serviceId,
            serviceDomain,
            serviceInstance,
            new MemoryCertificateStorage(),
            distinguishedName,
            certificateValidityInDays
        );
    }

    /**
     * Wrapper class for parameters for initial ship node configuration. If a
     * certificate should be loaded or stored, use the other constructor instead
     *
     * @param ipAddresses
     *     ip addresses of network interfaces to bind this Ship node to, example:
     *     ["192.168.1.2" /* ethernet * /, "::1" /* loopback * /]
     * @param port
     *     port for initial server
     * @param wssPath
     *     wss path for initial server, example: "/ship/"
     * @param keepAlive
     *     indicates if keepAlive packets are allowed
     * @param serviceId
     *     host name for JmDNS instance (service discovery), example:
     *     "EXAMPLEBRAND-EEB01M3EU-001122334455"
     * @param serviceDomain
     *     which domain for service types to listen to, example: "local"
     * @param serviceInstance
     *     service instance label for initial server, example: "Dishwasher
     *     ExampleCompany EEB01M3EU"
     * @param distinguishedName
     *     X.509 Distinguished Name, eg "CN=Test, L=London, C=GB". For IoT devices,
     *     usually the DeviceID
     * @param certificateValidityInDays
     *     how many days the certificate should be valid for
     */
    private ShipNodeConfiguration(
        Set<String> ipAddresses,
        int port,
        String wssPath,
        boolean keepAlive,
        String serviceId,
        String serviceDomain,
        String serviceInstance,
        String distinguishedName,
        int certificateValidityInDays
    ) {
        this.ipAddresses = ipAddresses.stream().map(str -> {
            try {
                return InetAddress.getByName(str);
            }
            catch (UnknownHostException e) {
                // If an invalid ipAddress is given, the Application is supposed to fail.
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
        this.ipAddress = this.ipAddresses.stream().findAny().orElse(null);
        this.port = port;
        this.wssPath = wssPath;
        this.keepAlive = keepAlive;
        this.serviceId = serviceId;
        this.serviceDomain = serviceDomain;
        this.serviceInstance = serviceInstance;
        this.certificateStorage = new MemoryCertificateStorage();
        this.distinguishedName = distinguishedName;
        this.certificateValidityInDays = certificateValidityInDays;
    }

    public ShipNodeConfiguration(
        String ipAddress,
        int port,
        String wssPath,
        boolean keepAlive,
        String serviceId,
        String serviceDomain,
        String serviceInstance,
        String alias,
        char[] keyStorePassphrase,
        char[] keyPairPassphrase,
        String distinguishedName,
        int certificateValidityInDays
    ) {
        this(ipAddress == null ? Set.of() : Set.of(ipAddress), port, wssPath, keepAlive, serviceId, serviceDomain,
            serviceInstance, alias, keyStorePassphrase, keyPairPassphrase,
            distinguishedName, certificateValidityInDays);
    }

    /**
     * See
     * {@link #ShipNodeConfiguration(String inetAddr, int port, String wssPath,
     * boolean keepAlive, String serviceId, String serviceDomain, String
     * serviceInstance, String alias, char[] keyStorePassphrase, char[]
     * keyPairPassphrase, String distinguishedName, int days) ShipNodeConfiguration}. This
     * constructor additionally takes a path pointing to an existing certificate, and
     * an ipAddress. For example: "certs/keystore.jks" or
     * "C:/User/Test/certs/keystore.jks". If there is no certificate found at the
     * location, a new certificate will be created in the {@link ShipNodeImpl} class at
     * the location
     *
     * @param ipAddress
     *     IP address of network interface to bind this Ship node to, example:
     *     "192.168.1.2". If set to "0.0.0.0" this Ship node will bind to all
     *     available network interfaces.
     * @param port
     *     port for initial server
     * @param wssPath
     *     wss path for initial server, example: "/ship/"
     * @param keepAlive
     *     indicates if keepAlive packets are allowed
     * @param serviceId
     *     host name for JmDNS instance (service discovery), example:
     *     "EXAMPLEBRAND-EEB01M3EU-001122334455"
     * @param serviceDomain
     *     which domain for service types to listen to, example: "local"
     * @param serviceInstance
     *     service instance label for initial server, example: "Dishwasher
     *     ExampleCompany EEB01M3EU"
     * @param alias
     *     the alias for the key pair that should be loaded or created
     * @param certPath
     *     path pointing to an existing certificate
     * @param keyStorePassphrase
     *     passphrase for the whole key store, see KeyManagement class
     * @param keyPairPassphrase
     *     passphrase for the key pair to be generated
     * @param distinguishedName
     *     X.509 Distinguished Name, eg "CN=Test, L=London, C=GB". For IoT devices,
     *     usually the DeviceID
     * @param certificateValidityInDays
     *     how many days the certificate should be valid for
     */
    public ShipNodeConfiguration(
        String ipAddress,
        int port,
        String wssPath,
        boolean keepAlive,
        String serviceId,
        String serviceDomain,
        String serviceInstance,
        String alias,
        String certPath,
        char[] keyStorePassphrase,
        char[] keyPairPassphrase,
        String distinguishedName,
        int certificateValidityInDays
    ) {
        this(
            ipAddress,
            port,
            wssPath,
            keepAlive,
            serviceId,
            serviceDomain,
            serviceInstance,
            alias,
            keyStorePassphrase,
            keyPairPassphrase,
            distinguishedName,
            certificateValidityInDays
        );
        this.certificateStorage = new KeyStoreCertificateStorage(
            certPath,
            alias,
            keyStorePassphrase,
            keyPairPassphrase
        );
    }

    /**
     * See
     * {@link #ShipNodeConfiguration(String inetAddr, int port, String wssPath,
     * boolean keepAlive, String serviceId, String serviceDomain, String
     * serviceInstance, String alias, char[] keyStorePassphrase, char[]
     * keyPairPassphrase, String distinguishedName, int days) ShipNodeConfiguration}. This
     * constructor additionally takes a path pointing to an existing certificate, and
     * an ipAddress. For example: "certs/keystore.jks" or
     * "C:/User/Test/certs/keystore.jks". If there is no certificate found at the
     * location, a new certificate will be created in the {@link ShipNodeImpl} class at
     * the location
     *
     * @param ipAddresses
     *     ip addresses of network interfaces to bind this Ship node to, example:
     *     ["192.168.1.2" /* ethernet * /, "::1" /* loopback * /]
     * @param port
     *     port for initial server
     * @param wssPath
     *     wss path for initial server, example: "/ship/"
     * @param keepAlive
     *     indicates if keepAlive packets are allowed
     * @param serviceId
     *     host name for JmDNS instance (service discovery), example:
     *     "EXAMPLEBRAND-EEB01M3EU-001122334455"
     * @param serviceDomain
     *     which domain for service types to listen to, example: "local"
     * @param serviceInstance
     *     service instance label for initial server, example: "Dishwasher
     *     ExampleCompany EEB01M3EU"
     * @param alias
     *     the alias for the key pair that should be loaded or created
     * @param certPath
     *     path pointing to an existing certificate
     * @param keyStorePassphrase
     *     passphrase for the whole key store, see KeyManagement class
     * @param keyPairPassphrase
     *     passphrase for the key pair to be generated
     * @param distinguishedName
     *     X.509 Distinguished Name, eg "CN=Test, L=London, C=GB". For IoT devices,
     *     usually the DeviceID
     * @param certificateValidityInDays
     *     how many days the certificate should be valid for
     */
    public ShipNodeConfiguration(
        Set<String> ipAddresses,
        int port,
        String wssPath,
        boolean keepAlive,
        String serviceId,
        String serviceDomain,
        String serviceInstance,
        String alias,
        String certPath,
        char[] keyStorePassphrase,
        char[] keyPairPassphrase,
        String distinguishedName,
        int certificateValidityInDays
    ) {
        this(
            ipAddresses,
            port,
            wssPath,
            keepAlive,
            serviceId,
            serviceDomain,
            serviceInstance,
            alias,
            keyStorePassphrase,
            keyPairPassphrase,
            distinguishedName,
            certificateValidityInDays
        );
        this.certificateStorage = new KeyStoreCertificateStorage(
            certPath,
            alias,
            keyStorePassphrase,
            keyPairPassphrase
        );
    }

    /**
     * See
     * {@link #ShipNodeConfiguration(String ipAddress, int port, String wssPath,
     * boolean keepAlive, String serviceId, String serviceDomain, String
     * serviceInstance, String alias, char[] keyStorePassphrase, char[]
     * keyPairPassphrase, String distinguishedName, int days) ShipNodeConfiguration}. This
     * constructor additionally takes a path pointing to an existing certificate and
     * sets the ipAddress to null. For example: "certs/keystore.jks" or
     * "C:/User/Test/certs/keystore.jks". If there is no certificate found at the
     * location, a new certificate will be created in the {@link ShipNodeImpl} class at
     * the location. Wrapper class for parameters for initial ship node
     * configuration. If a certificate should be loaded or stored, use the other
     * constructor instead
     *
     * @param port
     *     port for initial server
     * @param wssPath
     *     wss path for initial server, example: "/ship/"
     * @param keepAlive
     *     indicates if keepAlive packets are allowed
     * @param serviceId
     *     host name for JmDNS instance (service discovery), example:
     *     "EXAMPLEBRAND-EEB01M3EU-001122334455"
     * @param serviceDomain
     *     which domain for service types to listen to, example: "local"
     * @param serviceInstance
     *     service instance label for initial server, example: "Dishwasher
     *     ExampleCompany EEB01M3EU"
     * @param alias
     *     the alias for the key pair that should be loaded or created
     * @param certPath
     *     path pointing to an existing certificate
     * @param keyStorePassphrase
     *     passphrase for the whole key store, see KeyManagement class
     * @param keyPairPassphrase
     *     passphrase for the key pair to be generated
     * @param distinguishedName
     *     X.509 Distinguished Name, eg "CN=Test, L=London, C=GB". For IoT devices,
     *     usually the DeviceID
     * @param certificateValidityInDays
     *     how many days the certificate should be valid for
     */
    public ShipNodeConfiguration(
        int port,
        String wssPath,
        boolean keepAlive,
        String serviceId,
        String serviceDomain,
        String serviceInstance,
        String alias,
        String certPath,
        char[] keyStorePassphrase,
        char[] keyPairPassphrase,
        String distinguishedName,
        int certificateValidityInDays
    ) {
        this(
            port,
            wssPath,
            keepAlive,
            serviceId,
            serviceDomain,
            serviceInstance,
            alias,
            keyStorePassphrase,
            keyPairPassphrase,
            distinguishedName,
            certificateValidityInDays
        );
        this.certificateStorage = new KeyStoreCertificateStorage(
            certPath,
            alias,
            keyStorePassphrase,
            keyPairPassphrase
        );
    }


    /**
     * See
     * {@link #ShipNodeConfiguration(String ipAddress, int port, String wssPath,
     * boolean keepAlive, String serviceId, String serviceDomain, String
     * serviceInstance, String alias, char[] keyStorePassphrase, char[]
     * keyPairPassphrase, String distinguishedName, int days) ShipNodeConfiguration}. this
     * constructor sets the ipAddress to null, so the ShipNode will take the IP
     * address of an arbitrary network interface to bind the connection to.
     *
     * @param port
     *     port for initial server
     * @param wssPath
     *     wss path for initial server, example: "/ship/"
     * @param keepAlive
     *     indicates if keepAlive packets are allowed
     * @param serviceId
     *     host name for JmDNS instance (service discovery), example:
     *     "EXAMPLEBRAND-EEB01M3EU-001122334455"
     * @param serviceDomain
     *     which domain for service types to listen to, example: "local"
     * @param serviceInstance
     *     service instance label for initial server, example: "Dishwasher
     *     ExampleCompany EEB01M3EU"
     * @param alias
     *     the alias for the key pair that should be loaded or created
     * @param keyStorePassphrase
     *     passphrase for the whole key store, see KeyManagement class
     * @param keyPairPassphrase
     *     passphrase for the key pair to be generated
     * @param distinguishedName
     *     X.509 Distinguished Name, eg "CN=Test, L=London, C=GB". For IoT devices,
     *     usually the DeviceID
     * @param certificateValidityInDays
     *     how many days the certificate should be valid for
     */
    public ShipNodeConfiguration(
        int port,
        String wssPath,
        boolean keepAlive,
        String serviceId,
        String serviceDomain,
        String serviceInstance,
        String alias,
        char[] keyStorePassphrase,
        char[] keyPairPassphrase,
        String distinguishedName,
        int certificateValidityInDays
    ) {
        this(
            (String) null,
            port,
            wssPath,
            keepAlive,
            serviceId,
            serviceDomain,
            serviceInstance,
            alias,
            keyStorePassphrase,
            keyPairPassphrase,
            distinguishedName,
            certificateValidityInDays
        );
    }

    public boolean isClientOnly() {
        return clientOnly;
    }

    public void setClientOnly(boolean clientOnly) {
        this.clientOnly = clientOnly;
    }

    /**
     *
     * @return the (singular) IP address that will be used for mDNS.
     * @deprecated because binding to multiple addresses is supported.
     *             Use {@link #getIpAddresses()} instead.
     */
    @Deprecated
    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public Set<InetAddress> getIpAddresses() {
        return ipAddresses;
    }

    /**
     *
     * @param ipAddress the IP address to bind this SHIP node to.
     * @deprecated because binding to multiple addresses is supported.
     *              Use {@link #setIpAddresses(Set)} instead.
     */
    @Deprecated
    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setIpAddresses(Set<InetAddress> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getWssPath() {
        return wssPath;
    }

    public void setWssPath(String wssPath) {
        this.wssPath = wssPath;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceDomain() {
        return serviceDomain;
    }

    public void setServiceDomain(String serviceDomain) {
        this.serviceDomain = serviceDomain;
    }

    public String getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(String serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public String getAlias() {
        return this.getKeyStoreCertStore()
            .map(KeyStoreCertificateStorage::getAlias)
            .orElse(null);
    }

    public String getCertPath() {
        return this.getKeyStoreCertStore()
            .map(KeyStoreCertificateStorage::getPathToKeyStore)
            .orElse(null);
    }

    public char[] getKeyStorePassphrase() {
        return this.getKeyStoreCertStore()
            .map(KeyStoreCertificateStorage::getKeyStorePassphrase)
            .orElse(null);
    }

    public char[] getKeyPairPassphrase() {
        return this.getKeyStoreCertStore()
            .map(KeyStoreCertificateStorage::getKeyPairPassphrase)
            .orElse(null);
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public int getCertificateValidityInDays() {
        return certificateValidityInDays;
    }

    public CertificateStorage getCertificateStorage() {
        return this.certificateStorage;
    }

    private Optional<KeyStoreCertificateStorage> getKeyStoreCertStore() {
        if (!(this.certificateStorage instanceof KeyStoreCertificateStorage)) {
            return Optional.empty();
        }
        return Optional.of((KeyStoreCertificateStorage) this.certificateStorage);
    }

    @Override
    public String toString() {
        return "ShipNodeConfiguration{" +
            "ipAddr=" + ipAddress +
            ", port=" + port +
            ", clientOnly=" + clientOnly +
            ", wssPath='" + wssPath + '\'' +
            ", keepAlive=" + keepAlive +
            ", serviceId='" + serviceId + '\'' +
            ", serviceDomain='" + serviceDomain + '\'' +
            ", serviceInstance='" + serviceInstance + '\'' +
            ", certificateStorage='" + certificateStorage + '\'' +
            ", dn='" + distinguishedName + '\'' +
            ", days=" + certificateValidityInDays +
            '}';
    }
}
