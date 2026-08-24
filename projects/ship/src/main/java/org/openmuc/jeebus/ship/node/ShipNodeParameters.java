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

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * This class holds a subset of constants defined in SHIP:4.2 SHIP Node Parameters.
 * Concretely, we only adopt parameters here which we use in the library and that
 * never change at runtime. For changable parameters, use {@link ConfigBuilder}.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public class ShipNodeParameters {

    public static final int MINIMAL_TRUST_LEVEL = 8;
    public static final int MAXIMAL_TRUST_LEVEL = 96;
    /**
     * trust level for user verified public keys
     */
    public final static int USER_VERIFIED_TRUST_LEVEL = 32;
    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 0;
    /**
     * the trust level for auto accept
     */
    public static final int AUTO_ACCEPT_TRUST_LEVEL = 8;
    /**
     * immutable list of supported protocols
     */
    public static final List<String> PROTOCOLS = singletonList("TLSv1.2");
    /**
     * immutable list of supported cipher suites
     */
    public static final List<String> CIPHERS = singletonList(
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256");
    public static final List<String> WELL_KNOWN_PROTOCOL_ID = singletonList("ee1.0");

    public static final int AUTO_ACCEPT_WINDOW = 60;
    public static final int CMI_TIMEOUT = 30;
    public static final int AMI_TIMEOUT = 60;

    // TODO: find out where this value is specified
    public static final int WSS_HANDSHAKE_TIMEOUT = 5;

    /**
     * T_hello_init=60-240, should be constant across connections
     * Wait-For-Ready-Timer initial
     */
    public static final int T_HELLO_INIT = 120;
    public static final List<String> SUPPORTED_FORMATS = singletonList("JSON-UTF8");

    public static int getTrustLevel(boolean isAutoAccept) {
        return isAutoAccept ? AUTO_ACCEPT_TRUST_LEVEL : 0;
    }

    private ShipNodeParameters(){
        // Initializing this class is pointless and thus forbidden.
    }
}
