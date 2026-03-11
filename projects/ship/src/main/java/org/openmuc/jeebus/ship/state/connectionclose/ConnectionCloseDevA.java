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

import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.connectionclose.CloseMsg;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;

import java.util.concurrent.TimeUnit;

public class ConnectionCloseDevA extends ConnectionClose {

    private boolean confirmationReceived;

    public ConnectionCloseDevA(ShipConnectionImpl connection, CloseMsg closeMessage) {
        super(connection, closeMessage);
        connection.sendRawMessage(
            ShipMessageFactory.parseConnectionCloseBody(closeMessage));
        if (closeMessage.getMaxTime() != null) {
            executor.schedule(
                () -> {
                    if (!confirmationReceived) {
                        this.connection.closeImmediately();
                    }
                },
                closeMessage.getMaxTime(),
                TimeUnit.MILLISECONDS
            );
        }
        executor.shutdown();
    }

    /**
     * Used when a confirmation was received. Closes the connection after setting the
     * variable
     */
    public void setConfirmationReceivedTrue() {
        this.confirmationReceived = true;
        connection.closeImmediately();
    }
}
