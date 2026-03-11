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

import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloPhaseType;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloType;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;
import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;

@StateHandler.Handles(State.SME_HELLO_STATE_READY_LISTEN)
@StateHandler.UsesExtraData(SmeExtraData.class)
public class ReadyListen implements StateHandler {
    @Override
    public void onEntered(State previous, StateHandlerContext context) {
        if (previous == State.SME_HELLO_STATE_PENDING_LISTEN) {
            if (context.getExtraData(SmeExtraData.class).partnerPhase == ConnectionHelloPhaseType.READY) {
                SmeHelloUtils.deactivateAllTimers(context);
                context.transitionTo(State.SME_HELLO_OK);
            } else {
                context.stopTimeouts(
                    SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY,
                    SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST
                );
            }
            SmeHelloUtils.sendHelloUpdateMessage(context);
        }
    }

    @Override
    public void onTimeoutExpired(SpecifiedTimeout kind, StateHandlerContext context) {
        if (kind == SpecifiedTimeout.SME_WAIT_FOR_READY) {
            context.transitionTo(State.SME_HELLO_STATE_READY_TIMEOUT);
        } else {
            StateHandler.super.onTimeoutExpired(kind, context);
        }
    }

    @Override
    public void onMessageReceived(StateHandlerContext context) {
        context.processMessage();
    }

    @Override
    public void processMessage(byte[] msg, StateHandlerContext context) {
        ConnectionHelloType hello;
        try {
            hello = MessageUtility
                .preprocessHelloMsg(msg)
                .getConnectionHello();
        } catch (IllegalArgumentException e) {
            SmeHelloUtils.abort(context);
            return;
        }
        SmeExtraData data = context.getExtraData(SmeExtraData.class);
        context.setExtraData(data.withNewMessage(hello));
        switch (hello.getPhase()) {
            case READY:
                context.transitionTo(State.SME_HELLO_OK);
                break;
            case PENDING:
                if (Boolean.TRUE.equals(hello.getProlongationRequest())) {
                    SmeHelloUtils.decideProlongationRequest(context);
                    SmeHelloUtils.sendHelloUpdateMessage(context);
                }  // else: ignore the message
                break;
            case ABORTED:
                SmeHelloUtils.abort(context);
        }
    }
}
