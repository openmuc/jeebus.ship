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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openmuc.jeebus.ship.state.machine.cmi.CommonMessages;
import org.openmuc.jeebus.ship.util.InstrumentedStateMachine;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openmuc.jeebus.ship.state.machine.State.*;
import static org.openmuc.jeebus.ship.util.StateMachineAssertions.*;

public class CmiStateTest {
    @Test
    public void initStartServer() {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(true);
        machine.begin();
        assertStateHistory(machine, CMI_INIT_START, CMI_STATE_SERVER_WAIT);
        assertNoMessages(machine);
    }

    @Test
    public void initStartClient() {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(false);
        machine.begin();
        assertStateHistory(machine, CMI_INIT_START, CMI_STATE_CLIENT_SEND, CMI_STATE_CLIENT_WAIT);
        assertLatestMessages(machine, CommonMessages.CMI_ZERO);
    }

    @Test
    public void serverWaitEvaluateOk() {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(true, CMI_STATE_SERVER_WAIT);
        machine.stoppingSet(Set.of(CONNECTION_DATA_PREPARATION));
        machine.messageReceived(CommonMessages.CMI_ZERO);
        assertLatestMessages(machine, CommonMessages.CMI_ZERO);
        assertStateHistory(machine, CMI_STATE_SERVER_WAIT, CMI_STATE_SERVER_EVALUATE, CONNECTION_DATA_PREPARATION);
    }

    @Test
    public void clientWaitEvaluateOk() {
        InstrumentedStateMachine machine = new InstrumentedStateMachine(true, CMI_STATE_SERVER_WAIT);
        machine.stoppingSet(Set.of(CONNECTION_DATA_PREPARATION));
        machine.messageReceived(CommonMessages.CMI_ZERO);
        assertStateHistory(machine, CMI_STATE_SERVER_WAIT, CMI_STATE_SERVER_EVALUATE, CONNECTION_DATA_PREPARATION);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void waitEvaluateBad(boolean isServer) {
        State initial = isServer ? CMI_STATE_SERVER_WAIT : CMI_STATE_CLIENT_WAIT;
        // complete nonsense message (bad MessageType)
        InstrumentedStateMachine machine = new InstrumentedStateMachine(isServer, initial);
        machine.messageReceived(new byte[]{1, 2, 3, 4});
        assertTrue(machine.isConnectionClosed());
        if (isServer) {
            assertLatestMessages(machine, CommonMessages.CMI_ZERO);
        } else {
            assertNoMessages(machine);
        }

        // bad CmiHead
        machine = new InstrumentedStateMachine(true, initial);
        machine.messageReceived(new byte[]{0, 77});
        assertTrue(machine.isConnectionClosed());
        if (isServer) {
            assertLatestMessages(machine, CommonMessages.CMI_ZERO);
        } else {
            assertNoMessages(machine);
        }
    }
}
