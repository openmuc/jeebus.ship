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

import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.api.DisconnectReason;
import org.openmuc.jeebus.ship.api.ShipConnectionInterface;
import org.openmuc.jeebus.ship.api.ShipService;

import java.util.HashMap;
import java.util.Map;

public class ConnectionSupervisor implements ConnectionHandler {

    private Map<Identity, ShipService> knownServices = new HashMap<>();

    @Override
    public void onMessageReceived(
        byte[] fullMsg,
        byte[] payload,
        ShipConnectionInterface shipConn
    ) {

    }

    @Override
    public void onDisconnect(
        DisconnectReason reason,
        ShipConnectionInterface shipConn
    ) {

    }

    @Override
    public void serviceAdded(ShipService service) {

    }

    @Override
    public void serviceRemoved(ShipService service) {

    }

    @Override
    public void clientConnected(ShipConnectionInterface connection) {

    }
}
