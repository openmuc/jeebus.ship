/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.util;

import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Assertions;
import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloPhaseType;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloType;
import org.openmuc.jeebus.ship.message.smeproth.ProtHError;
import org.openmuc.jeebus.ship.message.smeproth.ProtHErrorTypeAdapter;
import org.openmuc.jeebus.ship.message.smeproth.ProtocolHandshakeErrorMsg;
import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateMachine;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout.*;

/**
 * A collection of assertions about a {@link InstrumentedStateMachine}.
 */
public final class StateMachineAssertions {
    public static final SpecifiedTimeout[] SME_HELLO_TIMEOUTS = {
        SME_WAIT_FOR_READY,
        SME_SEND_PROLONGATION_REQUEST,
        SME_PROLONGATION_REQUEST_REPLY
    };

    private StateMachineAssertions() {}

    /**
     * Assert that the state machine is in the given state.
     * @param machine the state machine to check
     * @param state the state it should be in
     */
    public static void assertState(InstrumentedStateMachine machine, State state) {
        Assertions.assertEquals(state, machine.getState(), "machine should be in state " + state);
    }

    /**
     * Assert that the machine has gone through the given states, in order, since its
     * {@link StateMachine#begin()}.
     * @param machine the state machine to check
     * @param history ALL the states it should have been in
     */
    public static void assertStateHistory(InstrumentedStateMachine machine, State... history) {
        Assertions.assertIterableEquals(List.of(history), machine.getStateHistory(), "machine's state history should be " + Arrays.toString(
            history)
        );
    }

    /**
     * Assert that the machine has gone through the given states recently, i.e. only
     * the last N states are checked (where N is {@code expected.length}).
     * @param machine the state machine to check
     * @param expected the states it should have been in.
     */
    public static void assertStateHistoryTail(InstrumentedStateMachine machine, State... expected) {
        List<State> actual = machine.getStateHistory();
        if (actual.size() > expected.length) {
            actual = actual.subList(actual.size() - expected.length, actual.size());
        }
        Assertions.assertIterableEquals(List.of(expected), actual,
            String.format("machine's last %d states should be %s", expected.length,
                Arrays.toString(expected)
            ));
    }

    public static void assertNoMessages(InstrumentedStateMachine machine) {
        Assertions.assertIterableEquals(List.of(), machine.getMessagesSent(), "machine should not have sent any messages");
    }

    public static void assertLatestMessages(InstrumentedStateMachine machine, byte[]... expected) {
        List<byte[]> actual = machine.getMessagesSent();
        Assertions.assertFalse(actual.size() < expected.length,
            String.format("machine should have sent at least %d messages", expected.length));
        if (actual.size() > expected.length) {
            actual = actual.subList(actual.size() - expected.length, actual.size());
        }
        // cannot use Assertions.assertIterableEquals because <array>::equals()
        // doesn't compare contents
        for (int i = 0; i < expected.length; i++) {
            byte[] expectedMsg = expected[i];
            byte[] actualMsg = actual.get(i);
            Assertions.assertArrayEquals(expectedMsg, actualMsg,
                String.format("the %d'th message should match the expected value "
                    + "%s", i, Arrays.toString(expectedMsg)
                ));
        }
    }

    /**
     * Assert that the common procedure "abort" (SME Hello) was performed
     * @param machine the machine to check
     */
    public static void assertMachineAbortedSmeHello(InstrumentedStateMachine machine) {
        for (SpecifiedTimeout timeout : SME_HELLO_TIMEOUTS) {
            assertNull(machine.getTimeoutStatus(timeout),
                String.format("Timeout %s should not be running", timeout));
        }
        byte[] lastMessage = machine.getLatestMessage();
        ConnectionHelloType hello = MessageUtility.preprocessHelloMsg(lastMessage).getConnectionHello();

        assertEquals(ConnectionHelloPhaseType.ABORTED, hello.getPhase());
        assertNull(hello.getProlongationRequest());
        assertNull(hello.getWaiting());

        assertTrue(machine.isConnectionClosed(), "connection should be closed");
    }

    /**
     * Assert that the common procedure "abort" (Protocol Handshake) was performed
     * @param machine the machine to check
     */
    public static void assertMachineAbortedProtocolHandshake(InstrumentedStateMachine machine, ProtHError expectedError) {
        assertNull(machine.getTimeoutStatus(SME_PROTH_WAIT), "protocol handshake timer(s) should be stopped");
        byte[] lastMessage = machine.getLatestMessage();

        // manual decoding because there is no method for this in MessageUtility
        assertEquals(1, lastMessage[0], "message type should be 1");
        String body = new String(lastMessage, 1, lastMessage.length-1, StandardCharsets.UTF_8);
        ProtocolHandshakeErrorMsg protocolHandshakeErrorMsg
            = new GsonBuilder()
                .registerTypeAdapter(ProtocolHandshakeErrorMsg.class, new ProtHErrorTypeAdapter())
                .create()
                .fromJson(body, ProtocolHandshakeErrorMsg.class);

        assertEquals(expectedError, protocolHandshakeErrorMsg.getError());
    }

}
