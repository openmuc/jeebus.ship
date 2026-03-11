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
import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;

@StateHandler.Handles({State.CMI_STATE_CLIENT_WAIT, State.CMI_STATE_SERVER_WAIT})
public class ClientServerWait implements StateHandler {
    @Override
    public void onEntered(State previous, StateHandlerContext context) {
        context.startTimeout(SpecifiedTimeout.CMI_TIMEOUT);
    }

    @Override
    public void onBeforeExit(State pendingNext, StateHandlerContext context) {
        context.stopTimeouts(SpecifiedTimeout.CMI_TIMEOUT);
    }

    @Override
    public void onTimeoutExpired(SpecifiedTimeout kind, StateHandlerContext context) {
        if (kind != SpecifiedTimeout.CMI_TIMEOUT) {
            StateHandler.super.onTimeoutExpired(kind, context);
            return;
        }
        context.closeConnection();
    }

    @Override
    public void onMessageReceived(StateHandlerContext context) {
        switch (context.getState()) {
            case CMI_STATE_SERVER_WAIT:
                context.transitionTo(State.CMI_STATE_SERVER_EVALUATE);
                break;
            case CMI_STATE_CLIENT_WAIT:
                context.transitionTo(State.CMI_STATE_CLIENT_EVALUATE);
                break;
            default:
                throw new AssertionError("Unreachable state " + context.getState());
        }
    }
}
