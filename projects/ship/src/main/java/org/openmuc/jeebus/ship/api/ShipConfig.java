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

import java.net.*;
import java.util.Collections;
import java.util.Set;

/**
 * Holds the immutable configuration of a SHIP node.
 * <p>
 * The configuration is built by {@link ConfigBuilder} and passed to the node's
 * constructor. All fields are final, all collections immutable. After construction
 * the instance is thread‑safe.
 */
public class ShipConfig {

    private final String id;

    private final boolean serverEnabled;
    private final Set<InetSocketAddress> serverBindAddresses;
    private final boolean anyAddressEnabled;
    private final boolean autoAcceptEnabled;
    private final Set<String> trustedSkis;

    private final long networkInterfaceScanInitialDelay;
    private final long networkInterfaceScanInterval;

    private final String mDnsServiceInstance;
    private final String mDnsDomain;
    private final String brand;
    private final String type;
    private final String model;

    private final CertificateStorage certificateStorage;
    private final int certificateValidity;
    private final String certificateDistinguishedName;

    private final String wssPath;
    private final boolean keepAlive;

    private final AutoConnectPolicy autoConnectPolicy;
    private final long reconnectInitialDelay;
    private final long reconnectMaxDelay;
    private final double reconnectBackoffFactor;
    private final long reconnectDelayAfterRegularClose;

    public ShipConfig(
        String id,
        boolean serverEnabled,
        Set<InetSocketAddress> serverBindAddresses,
        boolean anyAddressEnabled,
        boolean autoAcceptEnabled,
        Set<String> trustedSkis,
        long networkInterfaceScanInitialDelay,
        long networkInterfaceScanInterval,
        String mDnsServiceInstance,
        String mDnsDomain,
        String brand,
        String type,
        String model,
        CertificateStorage certificateStorage,
        int certificateValidity,
        String certificateDistinguishedName,
        String wssPath,
        boolean keepAlive,
        AutoConnectPolicy autoConnectPolicy,
        long reconnectInitialDelay,
        long reconnectMaxDelay,
        double reconnectBackoffFactor,
        long reconnectDelayAfterRegularClose
    ) {
        this.id = id;
        this.serverEnabled = serverEnabled;
        this.serverBindAddresses = Collections.unmodifiableSet(serverBindAddresses);
        this.anyAddressEnabled = anyAddressEnabled;
        this.autoAcceptEnabled = autoAcceptEnabled;
        this.trustedSkis = Collections.unmodifiableSet(trustedSkis);
        this.networkInterfaceScanInitialDelay = networkInterfaceScanInitialDelay;
        this.networkInterfaceScanInterval = networkInterfaceScanInterval;
        this.mDnsServiceInstance = mDnsServiceInstance;
        this.mDnsDomain = mDnsDomain;
        this.brand = brand;
        this.type = type;
        this.model = model;
        this.certificateStorage = certificateStorage;
        this.certificateValidity = certificateValidity;
        this.certificateDistinguishedName = certificateDistinguishedName;
        this.wssPath = wssPath;
        this.keepAlive = keepAlive;
        this.autoConnectPolicy = autoConnectPolicy;
        this.reconnectInitialDelay = reconnectInitialDelay;
        this.reconnectMaxDelay = reconnectMaxDelay;
        this.reconnectBackoffFactor = reconnectBackoffFactor;
        this.reconnectDelayAfterRegularClose = reconnectDelayAfterRegularClose;
    }

    /**
     * Creates a new {@link ConfigBuilder} that can be used to construct a
     * {@link ShipConfig} instance.
     *
     * @return a new {@link ConfigBuilder}
     */
    public static ConfigBuilder getBuilder() {
        return new ConfigBuilder();
    }

    /**
     * @return whether keep‑alive is enabled for the server.
     */
    public boolean isKeepAlive() {
        return keepAlive;
    }

    public String getWssPath() {
        return wssPath;
    }

    /**
     * @return the policy that defines to which remote SHIP nodes this node connects
     *     on its own.
     */
    public AutoConnectPolicy getAutoConnectPolicy() {
        return autoConnectPolicy;
    }

    /**
     * @return the delay in milliseconds before the first reconnection attempt.
     */
    public long getReconnectInitialDelay() {
        return reconnectInitialDelay;
    }

    /**
     * @return the upper bound in milliseconds for the exponential reconnection
     *     backoff.
     */
    public long getReconnectMaxDelay() {
        return reconnectMaxDelay;
    }

    /**
     * @return the factor by which the reconnection delay grows per attempt.
     */
    public double getReconnectBackoffFactor() {
        return reconnectBackoffFactor;
    }

    /**
     * @return the delay in milliseconds before a reconnection attempt after a regular
     *     connection termination.
     */
    public long getReconnectDelayAfterRegularClose() {
        return reconnectDelayAfterRegularClose;
    }

    public CertificateStorage getCertificateStorage() {
        return certificateStorage;
    }

    public String getCertificateDistinguishedName() {
        return certificateDistinguishedName;
    }

    public int getCertificateValidity() {
        return certificateValidity;
    }

    public long getNetworkInterfaceScanInterval() {
        return networkInterfaceScanInterval;
    }

    public long getNetworkInterfaceScanInitialDelay() {
        return networkInterfaceScanInitialDelay;
    }

    public String getmDnsDomain() {
        return mDnsDomain;
    }

    public String getmDnsServiceInstance() {
        return mDnsServiceInstance;
    }

    public boolean getAutoAcceptEnabled() {
        return autoAcceptEnabled;
    }

    public boolean getServerEnabled() {
        return serverEnabled;
    }

    public Set<InetSocketAddress> getServerBindAddresses() {
        return serverBindAddresses;
    }

    public boolean getAnyAddressEnabled() {
        return anyAddressEnabled;
    }

    public String getId() {
        return id;
    }

    public Set<String> getTrustedSkis() {
        return trustedSkis;
    }

    public String getModel() {
        return model;
    }

    public String getType() {
        return type;
    }

    public String getBrand() {
        return brand;
    }

    @Override
    public String toString() {
        return String.format(
            "ShipConfig{id='%s', serverEnabled=%s, serverBindAddresses=%s, anyAddressEnabled=%s, autoAcceptEnabled=%s, trustedSkis=%s, networkInterfaceScanInitialDelay=%d, networkInterfaceScanInterval=%d, mDnsServiceInstance='%s', mDnsDomain='%s', brand='%s', type='%s', model='%s', certificateStorage=%s, certificateValidity=%d, certificateDistinguishedName='%s', wssPath='%s', keepAlive=%s, autoConnectPolicy=%s, reconnectInitialDelay=%d, reconnectMaxDelay=%d, reconnectBackoffFactor=%s, reconnectDelayAfterRegularClose=%d}",
            id,
            serverEnabled,
            serverBindAddresses,
            anyAddressEnabled,
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
            keepAlive,
            autoConnectPolicy,
            reconnectInitialDelay,
            reconnectMaxDelay,
            reconnectBackoffFactor,
            reconnectDelayAfterRegularClose
        );
    }
}
