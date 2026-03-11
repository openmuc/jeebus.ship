/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.machine;

import org.openmuc.jeebus.ship.state.machine.cde.ConnectionDataExchange;
import org.openmuc.jeebus.ship.state.machine.close.CloseDevA;
import org.openmuc.jeebus.ship.state.machine.close.CloseDevB;
import org.openmuc.jeebus.ship.state.machine.cmi.*;
import org.openmuc.jeebus.ship.state.machine.pinv.InitListen;
import org.openmuc.jeebus.ship.state.machine.pinv.PinOk;
import org.openmuc.jeebus.ship.state.machine.smehello.*;
import org.openmuc.jeebus.ship.state.machine.smeproth.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * All states that a SHIP connection might be in.
 */
public enum State {
    // note: these enum constants are in REVERSE order relative to the SHIP specification.
    // this is because each state specifies which states it might transition into, so
    // those later states must be declared first.

    // Closing is also handled specially
    CLOSING_DEV_A(
        new CloseDevA()
    ),
    CLOSING_DEV_B(
        new CloseDevB()
    ),

    // Connection Data Exchange is handled specially (probably)
    CONNECTION_DATA_EXCHANGE(
        new ConnectionDataExchange(),
        CLOSING_DEV_A,
        CLOSING_DEV_B
    ),

    // PIN Verification
    // we deviate from the spec here: we don't support PIN exchange at all,
    // so we just have to tell the communication partner "I don't need a PIN"
    // and await their "I don't need a PIN" message.
    //
    // it doesn't seem like anybody uses PINs anyway (neither we nor the eebus-go
    // developer have ever encountered one)
    SME_PIN_STATE_OK(
        new PinOk(),
        CONNECTION_DATA_EXCHANGE
    ),
    SME_PIN_STATE_INIT_LISTEN(
        new InitListen(),
        SME_PIN_STATE_OK
    ),

    // SME Protocol Handshake
    SME_PROT_H_STATE_CLIENT_OK(
        new ProtHOk(),
        SME_PIN_STATE_INIT_LISTEN
    ),
    SME_PROT_H_STATE_SERVER_OK(
        new ProtHOk(),
        SME_PIN_STATE_INIT_LISTEN
    ),
    SME_PROT_H_STATE_TIMEOUT(
        new Timeout()
    ),
    SME_PROT_H_STATE_SERVER_LISTEN_CONFIRM(
        new ServerListenConfirm(),
        SME_PROT_H_STATE_SERVER_OK,
        SME_PROT_H_STATE_TIMEOUT
    ),
    SME_PROT_H_STATE_CLIENT_LISTEN_CHOICE(
        new ClientListenChoice(),
        SME_PROT_H_STATE_CLIENT_OK,
        SME_PROT_H_STATE_TIMEOUT
    ),
    SME_PROT_H_STATE_SERVER_LISTEN_PROPOSAL(
        new ServerListenProposal(),
        SME_PROT_H_STATE_SERVER_LISTEN_CONFIRM,
        SME_PROT_H_STATE_TIMEOUT
    ),
    SME_PROT_H_SERVER_INIT(
        new ServerInit(),
        SME_PROT_H_STATE_SERVER_LISTEN_PROPOSAL
    ),
    SME_PROT_H_CLIENT_INIT(
        new ClientInit(),
        SME_PROT_H_STATE_CLIENT_LISTEN_CHOICE
    ),

    // SME Hello
    SME_HELLO_OK(
        new HelloOk(),
        SME_PROT_H_SERVER_INIT,
        SME_PROT_H_CLIENT_INIT
    ),
    SME_HELLO_STATE_READY_TIMEOUT(
        new ReadyTimeout()
    ),
    SME_HELLO_STATE_READY_LISTEN(
        new ReadyListen(),
        SME_HELLO_OK,
        SME_HELLO_STATE_READY_TIMEOUT
    ),
    SME_HELLO_STATE_READY_INIT(
        new ReadyInit(),
        SME_HELLO_STATE_READY_LISTEN
    ),
    SME_HELLO_STATE_PENDING_TIMEOUT(
        new PendingTimeout(),
        "SME_HELLO_STATE_PENDING_LISTEN"
    ),
    SME_HELLO_STATE_PENDING_LISTEN(
        new PendingListen(),
        SME_HELLO_STATE_PENDING_TIMEOUT,
        SME_HELLO_STATE_READY_LISTEN
    ),
    SME_HELLO_STATE_PENDING_INIT(
        new PendingInit(),
        SME_HELLO_STATE_PENDING_LISTEN
    ),

    // CMI
    CONNECTION_DATA_PREPARATION(
        new ConnectionDataPreparation(),
        SME_HELLO_STATE_PENDING_INIT,
        SME_HELLO_STATE_READY_INIT
    ),
    CMI_STATE_CLIENT_EVALUATE(
        new ClientEvaluate(),
        CONNECTION_DATA_PREPARATION
    ),
    CMI_STATE_CLIENT_WAIT(
        new ClientServerWait(),
        CMI_STATE_CLIENT_EVALUATE
    ),
    CMI_STATE_CLIENT_SEND(
        new ClientSend(),
        CMI_STATE_CLIENT_WAIT
    ),
    CMI_STATE_SERVER_EVALUATE(
        new ServerEvaluate(),
        CONNECTION_DATA_PREPARATION
    ),
    CMI_STATE_SERVER_WAIT(
        new ClientServerWait(),
        CMI_STATE_SERVER_EVALUATE
    ),
    CMI_INIT_START(
        new CmiInitStart(),
        CMI_STATE_CLIENT_SEND,
        CMI_STATE_SERVER_WAIT
    );

    private final StateHandler handler;
    // intentionally non-final because we have to fix up this data in the static{} block
    // it is never changed after class initialization
    private Set<Object> allowedTransitions;

    static {
        assert checkHandlerAnnotations();
        rewriteAllowedTransitions();
    }

    State(StateHandler handler, Object... allowedTransitions) {
        this.handler = handler;
        Set<Object> set = new HashSet<>(allowedTransitions.length);
        set.addAll(Arrays.asList(allowedTransitions));
        this.allowedTransitions = set;
    }

    /**
     *
     * @return the event handler for this state.
     */
    public StateHandler getHandler() {
        return handler;
    }

    /**
     *
     * @return the set of states that this state might transition into.
     */
    @SuppressWarnings("unchecked")
    public Set<State> getAllowedTransitions() {
        return (Set<State>) (Set<?>) allowedTransitions;
    }

    private static boolean checkHandlerAnnotations() {
        for (State state : values()) {
            if (state.getHandler() == StateHandler.UNIMPLEMENTED) {
                continue;
            }
            StateHandler.Handles annotation = state
                .getHandler()
                .getClass()
                .getAnnotation(StateHandler.Handles.class);
            if (annotation == null) {
                throw new IllegalArgumentException("Handler for state "
                    + state
                    + " does not have @StateHandler.Handles annotation!");
            }
            if (Arrays.stream(annotation.value()).noneMatch(it -> it == state)) {
                throw new IllegalArgumentException("Handler for state "
                    + state
                    + " does not contain this state in its @StateHandler.Handles "
                    + "annotation!");
            }
        }
        return true;
    }

    private static void rewriteAllowedTransitions() {
        for (State state : values()) {
            state.allowedTransitions = state.allowedTransitions.stream().map(o -> {
                if (o instanceof State) {
                    return o;
                }
                else if (o instanceof String) {
                    return State.valueOf((String) o);
                }
                else {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
        }
    }
}
