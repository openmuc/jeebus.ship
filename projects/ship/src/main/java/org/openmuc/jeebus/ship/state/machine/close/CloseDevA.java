/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.machine.close;

import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.connectionclose.CloseMsg;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;
import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;

import java.util.concurrent.TimeUnit;

@StateHandler.Handles(State.CLOSING_DEV_A)
@StateHandler.UsesExtraData(CloseMsg.class)
public class CloseDevA implements StateHandler {
    @Override
    public void onEntered(State previous, StateHandlerContext context) {
        CloseMsg closeMsg = context.getExtraData(CloseMsg.class);
        context.sendMessage(ShipMessageFactory.parseConnectionCloseBody(closeMsg));
        if (closeMsg.getMaxTime() != null) {
            context.startTimeout(
                SpecifiedTimeout.CLOSE_WAIT,
                closeMsg.getMaxTime(),
                TimeUnit.MILLISECONDS
            );
        }
    }
}
