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

import org.openmuc.jeebus.ship.api.ConfigBuilder;
import org.openmuc.jeebus.ship.api.cert.CertificateStorage;

import java.net.*;
import java.util.Collections;
import java.util.Set;

/**
 * Holds the immutable configuration of a SHIP node.
 * <p>
 * The configuration is built by {@link ConfigBuilder} and passed to the node's
 * constructor. All fields are final, all collections immutable. After construction
 * the instance is thread‑safe.</p>
 */
public final class ShipConfig {

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
        boolean keepAlive
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
            "ShipConfig{id='%s', serverEnabled=%s, serverBindAddresses=%s, anyAddressEnabled=%s, autoAcceptEnabled=%s, trustedSkis=%s, networkInterfaceScanInitialDelay=%d, networkInterfaceScanInterval=%d, mDnsServiceInstance='%s', mDnsDomain='%s', brand='%s', type='%s', model='%s', certificateStorage=%s, certificateValidity=%d, certificateDistinguishedName='%s', wssPath='%s', keepAlive=%s}",
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
            keepAlive
        );
    }
}
