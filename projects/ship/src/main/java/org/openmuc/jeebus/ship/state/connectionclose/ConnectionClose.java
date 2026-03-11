/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.connectionclose;

import org.openmuc.jeebus.ship.message.connectionclose.CloseMsg;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class ConnectionClose {

    protected final Logger log = LoggerFactory.getLogger(ConnectionClose.class);

    protected ShipConnectionImpl connection;

    // used to either schedule closing of connection in DEV-A and time the maxTime in DEV-B
    protected ScheduledExecutorService executor
        = Executors.newSingleThreadScheduledExecutor();

    protected CloseMsg closeMessage;

    public ConnectionClose(ShipConnectionImpl connection, CloseMsg closeMessage) {
        this.connection = connection;
        this.closeMessage = closeMessage;
    }

}