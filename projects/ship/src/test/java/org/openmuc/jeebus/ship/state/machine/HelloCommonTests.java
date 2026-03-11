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

import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloPhaseType;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloType;
import org.openmuc.jeebus.ship.node.Configuration;
import org.openmuc.jeebus.ship.util.InstrumentedStateMachine;
import org.openmuc.jeebus.ship.util.StateMachineAssertions;
import org.openmuc.jeebus.ship.util.ThrowingUserInterface;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout.SME_WAIT_FOR_READY;

/**
 * Common test code shared between {@link HelloPendingStateTest} and {@link HelloReadyStateTest}.
 */
public class HelloCommonTests {
    public static final int DEFAULT_WAIT_FOR_READY = new Configuration().getT_hello_init();
    // why does the SHIP spec have two constants with different names that are
    // defined to have the same value? it is a mystery
    public static final int WAIT_FOR_READY_INCREMENT = DEFAULT_WAIT_FOR_READY;

    public static void readyTimeoutAborts(InstrumentedStateMachine machine) {
        machine.triggerEntered(null);

        machine.getTimeoutExecutor().tick(DEFAULT_WAIT_FOR_READY + 10, TimeUnit.SECONDS);

        StateMachineAssertions.assertMachineAbortedSmeHello(machine);
    }

    public static void permitsTwoProlongationRequestsThenObeysUser(InstrumentedStateMachine machine, ConnectionHelloPhaseType phase) {
        // permits one prolongation request, then denies further ones
        ThrowingUserInterface<Integer> ui = new ThrowingUserInterface<>() {
            { tracker = 0; }
            @Override
            public boolean promptProlongationRequest(String prefix) {
                tracker++;
                return tracker < 2;
            }
        };
        machine.setUserInterface(ui);

        byte[] prolongMsg = ShipMessageFactory.parseSmeHelloBody(
            ConnectionHelloPhaseType.PENDING, null, true);
        machine.getTimeoutExecutor().tick(DEFAULT_WAIT_FOR_READY/2, TimeUnit.SECONDS);

        // first prolongation request
        machine.messageReceived(prolongMsg);
        ConnectionHelloType reply = MessageUtility
            .preprocessHelloMsg(machine.getLatestMessage()).getConnectionHello();
        assertEquals(phase, reply.getPhase());
        assertNull(reply.getProlongationRequest());
        assertTrue(reply.getWaiting() > WAIT_FOR_READY_INCREMENT);
        assertEquals(0, ui.tracker, "user was prompted");

        machine.getTimeoutExecutor().tick(WAIT_FOR_READY_INCREMENT, TimeUnit.SECONDS);

        // second prolongation request
        machine.messageReceived(prolongMsg);
        reply = MessageUtility.preprocessHelloMsg(machine.getLatestMessage()).getConnectionHello();
        assertEquals(phase, reply.getPhase());
        assertNull(reply.getProlongationRequest());
        assertTrue(reply.getWaiting() > WAIT_FOR_READY_INCREMENT);
        assertEquals(0, ui.tracker, "user was prompted");

        machine.getTimeoutExecutor().tick(WAIT_FOR_READY_INCREMENT, TimeUnit.SECONDS);

        // third prolongation request: user is prompted, accepts
        machine.messageReceived(prolongMsg);
        reply = MessageUtility.preprocessHelloMsg(machine.getLatestMessage()).getConnectionHello();
        assertEquals(phase, reply.getPhase());
        assertNull(reply.getProlongationRequest());
        assertTrue(reply.getWaiting() > WAIT_FOR_READY_INCREMENT);
        assertEquals(1, ui.tracker, "user wasn't prompted");

        machine.getTimeoutExecutor().tick(WAIT_FOR_READY_INCREMENT, TimeUnit.SECONDS);

        // fourth prolongation request: user is prompted, declines
        int prevWaitTime = machine.getTimeoutStatus(SME_WAIT_FOR_READY);
        machine.messageReceived(prolongMsg);
        assertEquals(prevWaitTime, machine.getTimeoutStatus(SME_WAIT_FOR_READY));
        reply = MessageUtility.preprocessHelloMsg(machine.getLatestMessage()).getConnectionHello();
        assertEquals(phase, reply.getPhase());
        assertNull(reply.getProlongationRequest());
        assertEquals(prevWaitTime, reply.getWaiting());
        assertEquals(2, ui.tracker, "user wasn't prompted");
    }
}
