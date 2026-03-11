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

import java.util.concurrent.TimeUnit;

public class ConnectionCloseDevB extends ConnectionClose {

    private boolean maxTimeExpired;

    public ConnectionCloseDevB(ShipConnectionImpl connection, CloseMsg closeMessage) {
        super(connection, closeMessage);
        if (closeMessage.getMaxTime() != null) {
            executor.schedule(
                () -> maxTimeExpired = true,
                closeMessage.getMaxTime(),
                TimeUnit.MILLISECONDS
            );
        }
        executor.shutdown();
    }

    public void prepareShutDown() {
        connection.prepareCDEShutdown();
    }

    public boolean isMaxTimeExpired() {
        return maxTimeExpired;
    }
}
