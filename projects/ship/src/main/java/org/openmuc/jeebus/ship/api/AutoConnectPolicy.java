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

/**
 * Defines to which remote SHIP nodes this node establishes connections on its own.
 * <p>
 * The policy covers both the initial connection attempt after a remote SHIP service
 * was discovered via mDNS/DNS-SD and the reconnection attempt after a connection was
 * terminated or interrupted.
 *
 * @see "SHIP:5 Service Discovery; SHIP:6 Connection Establishment; SHIP:13.4.7.1.2"
 */
public enum AutoConnectPolicy {
    /**
     * No automatic connection establishment and no reconnection. Connections are only
     * opened by the application itself.
     */
    NONE,
    /**
     * Automatic connection establishment only to SHIP nodes whose SKI is known with a
     * trust level of at least {@code MINIMAL_TRUST_LEVEL}. This is the default.
     *
     * @see "SHIP:12.3.2 Trust Level"
     */
    TRUSTED,
    /**
     * Automatic connection establishment to every discovered SHIP service, regardless
     * of its trust level.
     */
    ALL
}
