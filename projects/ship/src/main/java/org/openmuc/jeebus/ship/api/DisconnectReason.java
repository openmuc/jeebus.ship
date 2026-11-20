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
 * Reason why a SHIP connection was torn down, as reported to
 * {@link ConnectionHandler#onDisconnect(DisconnectReason,
 * ShipConnectionInterface)}.
 */
public enum DisconnectReason {
    /**
     * The connection was shut down in an orderly fashion on SHIP level, i.e. via the
     * connection close procedure (CDE) of the specification.
     */
    REGULAR_END,
    /**
     * The peer closed the underlying WebSocket transport with a normal status code
     * (1000 or 1001) without a preceding SHIP level shutdown.
     */
    REMOTE_CLOSED,
    /**
     * The connection was canceled because another connection to the same peer
     * already exists. Only one connection per peer is allowed.
     */
    DOUBLE_CONNECTION,
    /**
     * The connection was lost for any other reason, e.g. a protocol violation, a
     * timeout or an abnormal transport abort.
     */
    ERROR
}
