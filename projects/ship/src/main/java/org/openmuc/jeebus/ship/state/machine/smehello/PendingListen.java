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
import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloPhaseType;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloType;
import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;

import java.util.concurrent.TimeUnit;

@StateHandler.Handles(State.SME_HELLO_STATE_PENDING_LISTEN)
@StateHandler.UsesExtraData(SmeExtraData.class)
public class PendingListen implements StateHandler {

    // TODO add "become ready" functionality
    @Override
    public void onMessageReceived(StateHandlerContext context) {
        context.processMessage();
    }

    @Override
    public void processMessage(byte[] msg, StateHandlerContext context) {
        final ConnectionHelloType hello;
        try {
            hello = MessageUtility
                .preprocessHelloMsg(msg)
                .getConnectionHello();
        }
        catch (IllegalArgumentException e) {
            SmeHelloUtils.abort(context);
            return;
        }
        SmeExtraData data = context.getExtraData(SmeExtraData.class);
        context.setExtraData(data.withNewMessage(hello));
        switch (hello.getPhase()) {
            case READY:
                if (hello.getWaiting() != null) {
                    context.stopTimeouts(
                        SpecifiedTimeout.SME_WAIT_FOR_READY,
                        SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY
                    );
                    evaluateWaiting(hello.getWaiting(), context);
                }
                else {
                    SmeHelloUtils.abort(context);
                }
                break;
            case PENDING:
                if (hello.getWaiting() != null
                    && hello.getProlongationRequest() == null) {
                    context.stopTimeouts(SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY);
                    evaluateWaiting(hello.getWaiting(), context);
                    break;
                }
                else if (hello.getWaiting() == null
                    && hello.getProlongationRequest() != null) {
                    SmeHelloUtils.decideProlongationRequest(context);
                    SmeHelloUtils.sendHelloUpdateMessage(context);
                    break;
                }
                break;
            case ABORTED:
                SmeHelloUtils.abort(context);
                break;
        }
    }

    @Override
    public void onTimeoutExpired(
        SpecifiedTimeout kind,
        StateHandlerContext context
    ) {
        switch (kind) {
            case SME_WAIT_FOR_READY:
                SmeHelloUtils.abort(context);
                break;
            case SME_SEND_PROLONGATION_REQUEST:
                sendProlongationRequest(context);
                break;
            case SME_PROLONGATION_REQUEST_REPLY:
                context.transitionTo(State.SME_HELLO_STATE_PENDING_TIMEOUT);
                break;
            default:
                StateHandler.super.onTimeoutExpired(kind, context);
        }
    }

    private static void sendProlongationRequest(StateHandlerContext context) {
        byte[] helloMsg = ShipMessageFactory.parseSmeHelloBody(
            ConnectionHelloPhaseType.PENDING, null, true);
        context.sendMessage(helloMsg);

        int timeout = getRequestReplyWaitTime(context);
        context.startTimeout(
            SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY,
            timeout,
            TimeUnit.SECONDS
        );
    }

    private static int getRequestReplyWaitTime(StateHandlerContext context) {
        SmeExtraData extraData = context.getExtraData(SmeExtraData.class);
        if (extraData.lastWaiting != null) {
            return extraData.lastWaiting;
        }
        else {
            return context.getTimeoutStatus(SpecifiedTimeout.SME_WAIT_FOR_READY) * 11
                / 10;
        }
    }

    private void evaluateWaiting(int waiting, StateHandlerContext context) {
        if (waiting >= SmeHelloUtils.T_hello_prolong_thr_inc) {
            int newTimer = waiting - SmeHelloUtils.T_hello_prolong_waiting_gap;
            // spec specifies what to do if newTimer <= 0, but this
            // can't actually happen

            // restart the timeout
            context.stopTimeouts(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST);
            context.startTimeout(
                SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST,
                newTimer,
                TimeUnit.SECONDS
            );
        }
        else {
            context.stopTimeouts(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST);
        }
    }
}
