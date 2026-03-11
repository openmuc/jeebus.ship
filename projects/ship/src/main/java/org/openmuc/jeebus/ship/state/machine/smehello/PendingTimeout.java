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

import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloPhaseType;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;
import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;

@StateHandler.Handles(State.SME_HELLO_STATE_PENDING_TIMEOUT)
@StateHandler.UsesExtraData(SmeExtraData.class)
public class PendingTimeout implements StateHandler {
    private final byte[] PROLONGATION_REQUEST = ShipMessageFactory.parseSmeHelloBody(
        ConnectionHelloPhaseType.PENDING, null, true);

    private void startRequestReplyTimer(StateHandlerContext context) {
        SmeExtraData data = context.getExtraData(SmeExtraData.class);
        int timeout = data.lastWaiting != null
            ? data.lastWaiting
            : context.getTimeoutStatus(SpecifiedTimeout.SME_WAIT_FOR_READY) * 11 / 10;
        context.startTimeout(SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY, timeout);
    }

    @Override
    public void onEntered(State previous, StateHandlerContext context) {
        assert previous == State.SME_HELLO_STATE_PENDING_LISTEN;
        Integer wfr
            = context.getTimeoutStatus(SpecifiedTimeout.SME_WAIT_FOR_READY);
        if (wfr == null) {
            SmeHelloUtils.abort(context);
        } else {
            Integer spr
                = context.getTimeoutStatus(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST);
            if (spr != null) {
                // then we must be here because request-reply
                // timer expired
                SmeHelloUtils.abort(context);
            } else {
                context.sendMessage(PROLONGATION_REQUEST);
                startRequestReplyTimer(context);
                context.transitionTo(previous);
            }
        }
    }
}
