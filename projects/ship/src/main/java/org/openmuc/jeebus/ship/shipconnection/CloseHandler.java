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

import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.message.connectionclose.CloseMsg;
import org.openmuc.jeebus.ship.message.connectionclose.ConnectionCloseReasonType;
import org.openmuc.jeebus.ship.state.connectionclose.ConnectionCloseDevA;
import org.openmuc.jeebus.ship.state.connectionclose.ConnectionCloseDevB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openmuc.jeebus.ship.message.connectionclose.ConnectionClosePhaseType.ANNOUNCE;

public class CloseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloseHandler.class);

    private final ShipConnectionImpl connection;

    // state enabled after CDE if connection should be terminated, initiates graceful shutdown
    // devA is the side that initiates a termination
    private ConnectionCloseDevA devA;
    // devB is the side that receives a termination request
    private ConnectionCloseDevB devB;

    public CloseHandler(ShipConnectionImpl connection) {
        this.connection = connection;
    }

    public boolean isClosing() {
        return devA != null || devB != null;
    }

    public ConnectionCloseDevA getDevA() {
        return devA;
    }

    public ConnectionCloseDevB getDevB() {
        return devB;
    }

    public void initiate(Integer maxTime, ConnectionCloseReasonType reason) {

        if (devA != null) {
            LOGGER.warn("a connection termination was already initiated");
        }
        else {
            CloseMsg closeMsg = new CloseMsg(ANNOUNCE, maxTime, reason);
            devA = new ConnectionCloseDevA(connection, closeMsg);
        }
    }

    public void processMsg(byte[] message) {
        CloseMsg closeMsg
            = MessageUtility.preprocessConnCloseMsg(message);
        if (closeMsg.getPhase() == ANNOUNCE) {
            // the first termination request will be executed, afterwards requests will be ignored
            if (devB == null) {
                devB = new ConnectionCloseDevB(
                    connection,
                    closeMsg
                );
                devB.prepareShutDown();
            } else {
                LOGGER.warn(
                    "a connectionClose 'announce' message was received earlier, but received another one");
            }
        }
        else {
            if (devA != null) {
                devA.setConfirmationReceivedTrue();
            } else {
                LOGGER.warn(
                    "received a connectionClose 'confirm' message was received, but connection termination was never initiated");
            }
        }
    }
}