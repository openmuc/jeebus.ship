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

import org.openmuc.jeebus.ship.api.ShipService;
import org.openmuc.jeebus.ship.node.websocket.SkiManagementInfo;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class ShipPeer {
    private final String ski; // make primary identifier
    private final Set<ShipService> services = new HashSet<>();
    private Stack<ShipConnectionImpl> connections; // <- integrate start time + ready time; limit in size for spam reduction
    private SkiManagementInfo trust;

    public ShipPeer(String ski) {
        this.ski = ski;
    }

    public void addService(ShipService service) {
        services.add(service);
    }
}
