/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.machine.pinv;

import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.smepin.*;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@StateHandler.Handles(State.SME_PIN_STATE_INIT_LISTEN)
public class InitListen implements StateHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitListen.class);

    @Override
    public void onEntered(State previous, StateHandlerContext context) {
        // 1. send PIN requirement "none" (CHECK_INIT)
        context.sendMessage(ShipMessageFactory.parseSmePinBody(PinStateType.NONE, null));
    }

    @Override
    public void onMessageReceived(StateHandlerContext context) {
        context.processMessage();
    }

    @Override
    public void processMessage(byte[] msg, StateHandlerContext context) {
        PinValue pinValue = MessageUtility.preprocessPinMsg(msg).getPinValue();
        ConnectionPinStateType state = (ConnectionPinStateType) pinValue;
        switch (state.getPinState()) {
            case OPTIONAL:
            case REQUIRED:
                LOGGER.error("communication partner requires PIN, we don't support this feature!");
                context.closeConnection();
                return;
            case NONE:
            case PIN_OK:  // if the communication partner for some reason thinks
                          // that we gave a valid PIN, we don't question it :)
                context.transitionTo(State.SME_PIN_STATE_OK);
        }
    }
}
