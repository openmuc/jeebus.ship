/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.shipconnection;

import org.openmuc.jeebus.ship.api.ShipConnectionInterface;
import org.openmuc.jeebus.ship.view.UserInterface;

public interface ShipConnection extends ShipConnectionInterface {
    boolean isServer();

    void sendRawMessage(byte[] message);

    void closeImmediately();

    int getTrustLevel();

    void enableConnectionDataExchange();

    void setUserInterface(UserInterface userInterface);

    /**
     * Start up the SHIP state machine. Callable only once.
     */
    void initState();
}
