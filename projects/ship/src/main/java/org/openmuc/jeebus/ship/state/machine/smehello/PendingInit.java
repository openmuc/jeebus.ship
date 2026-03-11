/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.machine.smehello;

import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;

@StateHandler.Handles(State.SME_HELLO_STATE_PENDING_INIT)
@StateHandler.UsesExtraData(SmeExtraData.class)
public class PendingInit implements StateHandler {
    @Override
    public void onEntered(State previous, StateHandlerContext context) {
        context.setExtraData(new SmeExtraData());
        context.startTimeout(SpecifiedTimeout.SME_WAIT_FOR_READY);
        context.stopTimeouts(
            SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST,
            SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY
        );
        SmeHelloUtils.sendHelloUpdateMessage(context);
        context.transitionTo(State.SME_HELLO_STATE_PENDING_LISTEN);
    }
}
