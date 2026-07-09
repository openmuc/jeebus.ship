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
 */
public interface ClientConnectedListener {
    void onClientConnected(ShipConnection connection);
}
