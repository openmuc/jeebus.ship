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

@StateHandler.Handles(State.CMI_STATE_CLIENT_SEND)
public class ClientSend implements StateHandler {

    @Override
    public void onEntered(State previous, StateHandlerContext context) {
        context.sendMessage(CommonMessages.CMI_ZERO);
        context.transitionTo(State.CMI_STATE_CLIENT_WAIT);
    }
}
