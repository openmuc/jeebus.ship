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
import org.openmuc.jeebus.ship.message.smepin.PinInputPermissionType;
import org.openmuc.jeebus.ship.message.smepin.PinStateType;
import org.openmuc.jeebus.ship.state.machine.smehello.SmeExtraData;
import org.openmuc.jeebus.ship.util.InstrumentedStateMachine;
import org.openmuc.jeebus.ship.util.SHIPTestUtil;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.openmuc.jeebus.ship.state.machine.HelloCommonTests.DEFAULT_WAIT_FOR_READY;
import static org.openmuc.jeebus.ship.state.machine.State.*;
import static org.openmuc.jeebus.ship.util.StateMachineAssertions.*;

public class HelloPendingStateTest {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void untrustedGoesToPending(boolean isServer) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(
            isServer,
            CONNECTION_DATA_PREPARATION
        );
        machine.setTrustLevel(0);
        machine.triggerEntered(null);

        // correct states entered
        assertStateHistoryTail(
            machine,
            CONNECTION_DATA_PREPARATION,
            SME_HELLO_STATE_PENDING_INIT,
            SME_HELLO_STATE_PENDING_LISTEN
        );

        // timeouts
        assertEquals(
            DEFAULT_WAIT_FOR_READY,
            machine.getTimeoutStatus(SpecifiedTimeout.SME_WAIT_FOR_READY)
        );
        assertNull(machine.getTimeoutStatus(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST));
        assertNull(machine.getTimeoutStatus(SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY));

        // "hello update" message
        byte[] latestMessage = machine.getLatestMessage();
        ConnectionHelloType hello = MessageUtility
            .preprocessHelloMsg(latestMessage)
            .getConnectionHello();
        assertEquals(ConnectionHelloPhaseType.PENDING, hello.getPhase());
        assertEquals(DEFAULT_WAIT_FOR_READY, hello.getWaiting());
    }

    @ParameterizedTest
    @MethodSource("readyMsgStopsWaitForReadyTimer_Args")
    void readyMsgStopsWaitForReadyTimer(boolean isServer, int waiting) {

        InstrumentedStateMachine machine = new InstrumentedStateMachine(
            isServer,
            CONNECTION_DATA_PREPARATION
        );
        machine.setTrustLevel(0);
        machine.triggerEntered(null);

        assertState(machine, SME_HELLO_STATE_PENDING_LISTEN);

        // timer should be running initially
        assertEquals(
            DEFAULT_WAIT_FOR_READY,
            machine.getTimeoutStatus(SpecifiedTimeout.SME_WAIT_FOR_READY)
        );
        machine.startTimeout(SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY);

        machine.messageReceived(ShipMessageFactory.parseSmeHelloBody(
            ConnectionHelloPhaseType.READY,
            waiting,
            null
        ));

        assertNull(machine.getTimeoutStatus(SpecifiedTimeout.SME_WAIT_FOR_READY));
    }

    static Stream<Arguments> readyMsgStopsWaitForReadyTimer_Args() {
        return Stream.of(false, true)
            .flatMap(isServer -> Stream.of(
                Arguments.of(isServer, 10),
                Arguments.of(isServer, 100)
            ));
    }

    @ParameterizedTest
    @MethodSource("abortMsgCausesAbort_Args")
    void abortMsgCausesAbort(
        boolean isServer,
        boolean prolongationRequestTimerRunning,
        boolean requestReplyTimerRunning,
        byte[] abortMsg
    ) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(
            isServer,
            CONNECTION_DATA_PREPARATION
        );
        machine.setTrustLevel(0);
        machine.triggerEntered(null);

        assertState(machine, SME_HELLO_STATE_PENDING_LISTEN);
        if (prolongationRequestTimerRunning) {
            machine.startTimeout(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST, 25);
        }
        if (requestReplyTimerRunning) {
            machine.startTimeout(SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY, 25);
        }

        machine.messageReceived(abortMsg);
        assertMachineAbortedSmeHello(machine);
    }

    static Map<String, byte[]> ABORT_MESSAGES = Map.of(
        "phase=abort",
        ShipMessageFactory.parseSmeHelloBody(
            ConnectionHelloPhaseType.ABORTED,
            null,
            null
        ),
        "phase=ready,waiting=null",
        ShipMessageFactory.parseSmeHelloBody(
            ConnectionHelloPhaseType.READY,
            null,
            null
        ),
        "wrong_state",
        ShipMessageFactory.parseSmePinBody(
            PinStateType.REQUIRED,
            PinInputPermissionType.OK
        ),
        "nonsense_keysmash",
        "p3498y59-8hn5tyrkj 56".getBytes(StandardCharsets.UTF_8)
    );

    static Stream<Arguments> abortMsgCausesAbort_Args() {
        return ABORT_MESSAGES
            .entrySet()
            .stream()
            .flatMap(entry -> initialMachineStates().map(
                initial -> Arguments.argumentSet(
                    initial + entry.getKey(),
                    initial.isServer,
                    initial.prolongationRequestTimerRunning,
                    initial.requestReplyTimerRunning,
                    entry.getValue()
                )
            ));
    }

    static Stream<InitialMachineState> initialMachineStates() {
        return Stream.of(false, true)
            .flatMap(isServer ->
                Stream.of(
                    new InitialMachineState(isServer, false, false),
                    new InitialMachineState(isServer, true, false),
                    new InitialMachineState(isServer, false, true)
                )
            );
    }

    static class InitialMachineState {
        public final boolean isServer;
        public final boolean prolongationRequestTimerRunning;
        public final boolean requestReplyTimerRunning;

        public InitialMachineState(
            boolean isServer,
            boolean prolongationRequestTimerRunning,
            boolean requestReplyTimerRunning
        ) {
            this.isServer = isServer;
            this.prolongationRequestTimerRunning = prolongationRequestTimerRunning;
            this.requestReplyTimerRunning = requestReplyTimerRunning;
        }

        @Override
        public String toString() {
            return "{"
                +
                "isServer="
                + isServer
                +
                ", prolongationRequestTimerRunning="
                + prolongationRequestTimerRunning
                +
                ", requestReplyTimerRunning="
                + requestReplyTimerRunning
                +
                '}';
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void readyTimeoutAborts(boolean isServer) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, SME_HELLO_STATE_PENDING_INIT);
        HelloCommonTests.readyTimeoutAborts(machine);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pendingBecomesReadyOnCommand(boolean isServer) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, SME_HELLO_STATE_PENDING_INIT);
        machine.triggerEntered(null);

        becomesReadyWhenPartnerTrusted(machine, SME_HELLO_STATE_READY_LISTEN);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pendingWithReadyPartnerBecomesOk(boolean isServer) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, SME_HELLO_STATE_PENDING_INIT);
        machine.triggerEntered(null);
        machine.stoppingSet(Set.of(SME_HELLO_OK));

        machine.messageReceived(ShipMessageFactory.parseSmeHelloBody(
            ConnectionHelloPhaseType.READY,
            123,
            null
        ));

        becomesReadyWhenPartnerTrusted(machine, SME_HELLO_OK);
        assertNull(machine.getTimeoutStatus(SpecifiedTimeout.SME_WAIT_FOR_READY));
    }

    private static void becomesReadyWhenPartnerTrusted(InstrumentedStateMachine machine, State expectedState) {
        Integer prevWaiting = machine.getTimeoutStatus(SpecifiedTimeout.SME_WAIT_FOR_READY);

        machine.setCommPartnerTrusted();

        assertState(machine, expectedState);
        assertNull(machine.getTimeoutStatus(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST));
        assertNull(machine.getTimeoutStatus(SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY));

        ConnectionHelloType hello = MessageUtility
            .preprocessHelloMsg(machine.getLatestMessage())
            .getConnectionHello();
        assertEquals(ConnectionHelloPhaseType.READY, hello.getPhase());
        assertNull(hello.getProlongationRequest());
        assertEquals(prevWaiting, hello.getWaiting());
    }

    // first two prolongation requests are auto-accepted without asking the user
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void permitsTwoProlongationsThenObeysUser(boolean isServer) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, SME_HELLO_STATE_PENDING_INIT);
        machine.triggerEntered(null);

        // we're not testing these here, and we don't want them interfering with the test
        // so we just stop them
        machine.stopTimeouts(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST, SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY);

        HelloCommonTests.permitsTwoProlongationRequestsThenObeysUser(machine, ConnectionHelloPhaseType.PENDING);
    }

    @ParameterizedTest
    @MethodSource("messageWithWaitingSetsProlongationRequestTimer_args")
    void messageWithWaitingSetsProlongationRequestTimer(boolean isServer, ConnectionHelloPhaseType partnerPhase, int waiting) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, SME_HELLO_STATE_PENDING_INIT);
        machine.triggerEntered(null);

        byte[] msg = ShipMessageFactory.parseSmeHelloBody(
            partnerPhase,
            waiting,
            null
        );
        machine.messageReceived(msg);

        // constants and conditional taken from SHIP TS 1.0.1, section 13.4.4.1.3
        final int T_hello_prolong_thr_inc = 30; // seconds
        final int T_hello_prolong_waiting_gap = 15;  // seconds
        if (waiting < T_hello_prolong_thr_inc) {
            assertNull(machine.getTimeoutStatus(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST));
        } else {
            int actual = machine.getTimeoutStatus(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST);
            int expected = waiting - T_hello_prolong_waiting_gap;
            assertEquals(expected, actual);
        }
        assertNull(machine.getTimeoutStatus(SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY));
    }

    public static Stream<Arguments> messageWithWaitingSetsProlongationRequestTimer_args() {
        return SHIPTestUtil.streamProduct(
            () -> Stream.of(false, true),
            () -> Stream.of(ConnectionHelloPhaseType.READY, ConnectionHelloPhaseType.PENDING),
            () -> Stream.of(5, 120)
        ).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("prolongationRequestTimerSendsMessage_args")
    void prolongationRequestTimerSendsMessage(boolean isServer, int prevWaiting) {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, SME_HELLO_STATE_PENDING_INIT);
        machine.triggerEntered(null);

        // set wait-for-ready timer to some arbitrary large value, so it doesn't time out
        // while we're doing this test
        machine.stopTimeouts(SpecifiedTimeout.SME_WAIT_FOR_READY);
        machine.startTimeout(SpecifiedTimeout.SME_WAIT_FOR_READY, 0xbeef, TimeUnit.SECONDS);

        if (prevWaiting > 0) {
            machine.setExtraData(new SmeExtraData(0, prevWaiting, null));
        }

        machine.startTimeout(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST);
        // default duration is fine here
        assertEquals(10, machine.getTimeoutStatus(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST));

        // procedure under test
        machine.getTimeoutExecutor().tick(10, TimeUnit.SECONDS);

        // check that the correct message was sent
        ConnectionHelloType hello
            = MessageUtility.preprocessHelloMsg(machine.getLatestMessage()).getConnectionHello();
        assertEquals(ConnectionHelloPhaseType.PENDING, hello.getPhase());
        assertEquals(true, hello.getProlongationRequest());
        assertNull(hello.getWaiting());

        // check timer state
        assertEquals(0xbeef-10, machine.getTimeoutStatus(SpecifiedTimeout.SME_WAIT_FOR_READY));
        assertNull(machine.getTimeoutStatus(SpecifiedTimeout.SME_SEND_PROLONGATION_REQUEST));
        final int expectedTime = (prevWaiting > 0)
            ? prevWaiting
            : (0xbeef-10)*11/10;
        assertEquals(expectedTime, machine.getTimeoutStatus(SpecifiedTimeout.SME_PROLONGATION_REQUEST_REPLY));
    }

    public static Stream<Arguments> prolongationRequestTimerSendsMessage_args() {
        return SHIPTestUtil.streamProduct(
            ()-> Stream.of(false, true),
            ()-> Stream.of(73, -1)
        ).map(Arguments::of);
    }


}
