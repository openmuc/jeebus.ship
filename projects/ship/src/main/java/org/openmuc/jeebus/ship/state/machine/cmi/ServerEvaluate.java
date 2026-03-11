/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.machine.cmi;

import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@StateHandler.Handles(State.CMI_STATE_SERVER_EVALUATE)
public class ServerEvaluate implements StateHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerEvaluate.class);

    @Override
    public void onEntered(State previous, StateHandlerContext context) {
        context.processMessage();
    }

    @Override
    public void processMessage(byte[] msg, StateHandlerContext context) {
        if (msg.length < 2) {
            LOGGER.error("Received invalid message with length < 2");
            return;  // ignore empty message
        }
        byte msgType = msg[0];
        byte cmiHead = msg[1];
        if (msgType != 0) {
            LOGGER.error("Received invalid message with MessageType != 0");
            bye(context);
        } else if (cmiHead != 0) {
            LOGGER.error("Received invalid message with CmiHead != 0");
            bye(context);
        } else {
            context.sendMessage(CommonMessages.CMI_ZERO);
            context.transitionTo(State.CONNECTION_DATA_PREPARATION);
        }
    }

    private static void bye(StateHandlerContext context) {
        context.sendMessage(CommonMessages.CMI_ZERO);
        context.closeConnection();
    }
}
