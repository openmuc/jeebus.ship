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

import org.openmuc.jeebus.ship.api.AutoConnectPolicy;
import org.openmuc.jeebus.ship.api.cert.CertificateStorage;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * @deprecated since 3.2.2 to be replaced by {@link org.openmuc.jeebus.ship.api.ShipConfig}
 */
@Deprecated(since = "3.2.2", forRemoval = true)
public class ShipConfig extends org.openmuc.jeebus.ship.api.ShipConfig {
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
        super(
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
