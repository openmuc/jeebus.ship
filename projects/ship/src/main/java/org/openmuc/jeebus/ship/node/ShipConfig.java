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
import org.openmuc.jeebus.ship.util.Secret;

import java.net.*;
import java.util.Set;

public final class ShipConfig {

    private final String id;

    private final boolean serverEnabled;
    private final Set<InetSocketAddress> serverBindAddresses;
    private final boolean autoAcceptEnabled;
    private final Set<String> trustedSkis;

    private final String mDnsServiceInstance;
    private final String mDnsDomain;
    private final String brand;
    private final String type;
    private final String model;

    private final String keyStorePath;
    private final Secret keyStorePassphrase;
    private final String certificateAlias;
    private final int certificateValidity;
    private final String certificateDistinguishedName;
    private final Secret keyPairPassphrase;

    private final String wssPath;
    private final boolean keepAlive;

    public ShipConfig(
        String id,
        boolean serverEnabled,
        Set<InetSocketAddress> serverBindAddresses,
        boolean autoAcceptEnabled,
        Set<String> trustedSkis,
        String mDnsServiceInstance,
        String mDnsDomain,
        String brand,
        String type,
        String model,
        String keyStorePath,
        char[] keyStorePassphrase,
        String certificateAlias,
        int certificateValidity,
        String certificateDistinguishedName,
        char[] keyPairPassphrase,
        String wssPath,
        boolean keepAlive
    ) {
        this.id = id;
        this.serverEnabled = serverEnabled;
        this.serverBindAddresses = serverBindAddresses;
        this.autoAcceptEnabled = autoAcceptEnabled;
        this.trustedSkis = trustedSkis;
        this.mDnsServiceInstance = mDnsServiceInstance;
        this.mDnsDomain = mDnsDomain;
        this.brand = brand;
        this.type = type;
        this.model = model;
        this.keyStorePath = keyStorePath;
        this.keyStorePassphrase = new Secret(keyStorePassphrase);
        this.certificateAlias = certificateAlias;
        this.certificateValidity = certificateValidity;
        this.certificateDistinguishedName = certificateDistinguishedName;
        this.keyPairPassphrase = new Secret(keyPairPassphrase);
        this.wssPath = wssPath;
        this.keepAlive = keepAlive;
    }

    public static ConfigBuilder getBuilder() {
        return new ConfigBuilder();
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public String getWssPath() {
        return wssPath;
    }

    /**
     * Clears the passphrase after reading it for the first time.
     *
     * @return the passphrase for the key pair in the keystore
     */
    char[] getKeyPairPassphrase() {
        return keyPairPassphrase.consume();
    }

    public String getCertificateDistinguishedName() {
        return certificateDistinguishedName;
    }

    public int getCertificateValidity() {
        return certificateValidity;
    }

    public String getCertificateAlias() {
        return certificateAlias;
    }

    /**
     * Clears the passphrase after reading for the first time.
     *
     * @return the passphrase to the keystore
     */
    char[] getKeyStorePassphrase() {
        return keyStorePassphrase.consume();
    }

    public String getKeyStorePath() {
        return keyStorePath;
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
        return "ShipConfig{"
            + "id='"
            + id
            + '\''
            + ", serverEnabled="
            + serverEnabled
            + ", serverBindAddresses="
            + serverBindAddresses
            + ", autoAcceptEnabled="
            + autoAcceptEnabled
            + ", trustedSkis="
            + trustedSkis
            + ", mDnsServiceInstance='"
            + mDnsServiceInstance
            + '\''
            + ", mDnsDomain='"
            + mDnsDomain
            + '\''
            + ", brand='"
            + brand
            + '\''
            + ", type='"
            + type
            + '\''
            + ", model='"
            + model
            + '\''
            + ", keyStorePath='"
            + keyStorePath
            + '\''
            + ", certificateAlias='"
            + certificateAlias
            + '\''
            + ", certificateValidity="
            + certificateValidity
            + ", certificateDistinguishedName='"
            + certificateDistinguishedName
            + '\''
            + ", wssPath='"
            + wssPath
            + '\''
            + ", keepAlive="
            + keepAlive
            + '}';
    }

}
