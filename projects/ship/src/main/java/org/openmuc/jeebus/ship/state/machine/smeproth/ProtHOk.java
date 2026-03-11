/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.machine.smeproth;

import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;

@StateHandler.Handles({State.SME_PROT_H_STATE_CLIENT_OK, State.SME_PROT_H_STATE_SERVER_OK})
public class ProtHOk implements StateHandler {
    @Override
    public void onEntered(State previous, StateHandlerContext context) {
        context.stopTimeouts(SpecifiedTimeout.SME_PROTH_WAIT);
        context.transitionTo(State.SME_PIN_STATE_INIT_LISTEN);
    }
}
