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
import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;

/**
 * Common procedures and constants for SME states
 */
public final class SmeHelloUtils {

    private SmeHelloUtils() {
    }

    public static final byte[] ABORT_MSG = ShipMessageFactory.parseSmeHelloBody(
        ConnectionHelloPhaseType.ABORTED,
        null,
        null
    );

    public static void deactivateAllTimers(StateHandlerContext context) {
        context.stopTimeouts(
            SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST,
            SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY,
            SpecifiedTimeout.SME_WAIT_FOR_READY
        );
    }

    /**
     * Common "abort" procedure
     */
    public static void abort(StateHandlerContext context) {
        deactivateAllTimers(context);
        context.sendMessage(ABORT_MSG);
        context.closeConnection();
    }

    public static void increaseWfrTimer(StateHandlerContext context) {
        context.increaseTimeout(SpecifiedTimeout.SME_WAIT_FOR_READY);
    }

    /**
     * Common procedure to decide an incoming prolongation request
     */
    public static void decideProlongationRequest(StateHandlerContext context) {
        SmeExtraData data = context.getExtraData(SmeExtraData.class);
        int accepted = data.prolongationRequestsAccepted;
        // since the SHIP spec mandates we accept at least two prolongation
        // requests,
        // we don't bother asking the user for the first two requests
        if (accepted < 2) {
            context.setExtraData(data.withIncrementedRequests());
            increaseWfrTimer(context);
        }
        else if (context
            .getUserInterface()
            .promptProlongationRequest(String.format(
                "accept prolongation request number %s", accepted))) {
            increaseWfrTimer(context);
        }
    }

    /**
     * Common procedure for sending an SME "hello" Update Message
     *
     * @param context
     *     the current handler context
     */
    public static void sendHelloUpdateMessage(StateHandlerContext context) {
        Integer remaining
            = context.getTimeoutStatus(SpecifiedTimeout.SME_WAIT_FOR_READY);
        context.sendMessage(helloUpdateMessage(context.getState(), remaining));
    }

    public static byte[] helloUpdateMessage(State state, Integer timeRemaining) {
        ConnectionHelloPhaseType phase;
        switch (state) {
            case SME_HELLO_STATE_READY_INIT:
            case SME_HELLO_STATE_READY_LISTEN:
            case SME_HELLO_STATE_READY_TIMEOUT:
                phase = ConnectionHelloPhaseType.READY;
                break;
            case SME_HELLO_STATE_PENDING_INIT:
            case SME_HELLO_STATE_PENDING_LISTEN:
            case SME_HELLO_STATE_PENDING_TIMEOUT:
                phase = ConnectionHelloPhaseType.PENDING;
                break;
            default:
                throw new IllegalStateException("not an SME state:" + state);
        }
        return ShipMessageFactory.parseSmeHelloBody(phase, timeRemaining, null);
    }

    // timing constants from SHIP-spec 13.4.4.1.3, in seconds
    public static final int T_hello_prolong_thr_inc = 30;
    public static final int T_hello_prolong_waiting_gap = 15;
    public static final int T_hello_prolong_min = 60;

}
