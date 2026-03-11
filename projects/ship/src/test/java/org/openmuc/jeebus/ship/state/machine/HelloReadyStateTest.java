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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloPhaseType;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloType;
import org.openmuc.jeebus.ship.message.smeproth.ProtocolHandshakeTypeType;
import org.openmuc.jeebus.ship.util.InstrumentedStateMachine;
import org.openmuc.jeebus.ship.util.StateMachineAssertions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout.*;
import static org.openmuc.jeebus.ship.state.machine.State.*;
import static org.openmuc.jeebus.ship.util.StateMachineAssertions.assertState;
import static org.openmuc.jeebus.ship.util.StateMachineAssertions.assertStateHistoryTail;

public class HelloReadyStateTest {

    // initial state becomes READY if the communication partner is trusted
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void trustedGoesToReady(boolean isServer) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, CONNECTION_DATA_PREPARATION);
        machine.setTrustLevel(32);
        machine.triggerEntered(null);

        // correct states entered
        assertStateHistoryTail(machine, CONNECTION_DATA_PREPARATION, SME_HELLO_STATE_READY_INIT, SME_HELLO_STATE_READY_LISTEN);

        // timeouts
        assertEquals(
            HelloCommonTests.DEFAULT_WAIT_FOR_READY, machine.getTimeoutStatus(
            SME_WAIT_FOR_READY));
        assertNull(machine.getTimeoutStatus(SME_SEND_PROLONGATION_REQUEST));
        assertNull(machine.getTimeoutStatus(SME_PROLONGATION_REQUEST_REPLY));

        // "hello update" message
        byte[] latestMessage = machine.getLatestMessage();
        ConnectionHelloType hello = MessageUtility.preprocessHelloMsg(latestMessage).getConnectionHello();
        assertEquals(ConnectionHelloPhaseType.READY, hello.getPhase());
        assertEquals(HelloCommonTests.DEFAULT_WAIT_FOR_READY, hello.getWaiting());
    }

    // state READY goes to HELLO_OK when receiving a message with phase=READY
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void readyReadyGoesToOk(boolean isServer) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, SME_HELLO_STATE_READY_INIT);
        machine.triggerEntered(null);
        machine.stoppingSet(Set.of(SME_HELLO_OK));

        byte[] readyMsg = ShipMessageFactory.parseSmeHelloBody(ConnectionHelloPhaseType.READY, null, null);
        machine.messageReceived(readyMsg);
        assertState(machine, SME_HELLO_OK);
        assertTrue(machine.isPeerSkiAuthenticated());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void readyTimeoutAborts(boolean isServer) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, SME_HELLO_STATE_READY_INIT);
        HelloCommonTests.readyTimeoutAborts(machine);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void readyAbortMessageAborts(boolean isServer) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, SME_HELLO_STATE_READY_INIT);
        machine.triggerEntered(null);

        byte[] abortMsg = ShipMessageFactory.parseSmeHelloBody(ConnectionHelloPhaseType.ABORTED, null, null);

        machine.messageReceived(abortMsg);
        StateMachineAssertions.assertMachineAbortedSmeHello(machine);
    }

    @ParameterizedTest
    @MethodSource("readyBadMessageAborts_Args")
    void readyBadMessageAborts(boolean isServer, byte[] badMsg) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, SME_HELLO_STATE_READY_INIT);
        machine.triggerEntered(null);
        machine.messageReceived(badMsg);
        StateMachineAssertions.assertMachineAbortedSmeHello(machine);
    }

    public static Stream<Arguments> readyBadMessageAborts_Args() {
        return Stream.of(
            // arbitrary keyboard-smashed nonsense bytes
            new byte[] {87, 65, 6, 6},
            "984y64tbhjrgl.hn5".getBytes(StandardCharsets.UTF_8),
            // protocol handshake message
            ShipMessageFactory.parseSmeProtHBody(ProtocolHandshakeTypeType.SELECT, 1, 4, List.of("asn1"))
        ).flatMap(msg -> Stream.of(
            Arguments.of(false, msg),
            Arguments.of(true, msg)
        ));
    }

    // first two prolongation requests are auto-accepted without asking the user
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void readyPermitsTwoProlongationsThenObeysUser(boolean isServer) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, SME_HELLO_STATE_READY_INIT);
        machine.triggerEntered(null);
        HelloCommonTests.permitsTwoProlongationRequestsThenObeysUser(machine, ConnectionHelloPhaseType.READY);
    }
}
