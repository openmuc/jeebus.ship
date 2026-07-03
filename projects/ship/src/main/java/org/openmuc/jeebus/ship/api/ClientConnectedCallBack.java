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

import org.openmuc.jeebus.ship.shipconnection.ShipConnection;

/**
 * This listener can be used for the server to set a procedure to run as soon as a
 * client connects to that server.
 *
 * @deprecated since 2.3.0. Will be renamed to {@code ClientConnectedListener} in
 * 3.0.0
 */
@Deprecated(since = "2.3.0")
public interface ClientConnectedCallBack {
    void onClientConnected(ShipConnection connection);
}
