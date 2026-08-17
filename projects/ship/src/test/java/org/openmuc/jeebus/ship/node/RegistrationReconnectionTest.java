/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmuc.jeebus.ship.api.cert.CertificateStoreException;
import org.openmuc.jeebus.ship.api.cert.MemoryCertificateStorage;
import org.openmuc.jeebus.ship.node.websocket.AuthenticatedConnection;
import org.openmuc.jeebus.ship.shipconnection.ShipConnectionImpl;
import org.openmuc.jeebus.ship.state.machine.State;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.mockito.Mockito.when;

@Execution(SAME_THREAD)
@ExtendWith(MockitoExtension.class)
@Disabled("replace with non-global keymanagement and narrower tests")
public class RegistrationReconnectionTest {
    private final String exampleSki = "1234AAAAFFFF1111CCCC3333EEEEDDDD99992222";

    private final ShipNodeContext exampleCtx
        = new ShipNodeContext(
            new KeyManagement(
                new MemoryCertificateStorage(),
                "CN=test",
                "test",
                3650
            ),
            new ShipNodeParameters(),
            "some-id"
    );

    private ShipConnectionImpl exampleConn;

    @Mock
    private AuthenticatedConnection basicListenerMock;

    public RegistrationReconnectionTest() throws CertificateStoreException {
    }

    @BeforeEach
    public void setUp() {
        when(basicListenerMock.getPeerSki()).thenReturn(exampleSki);
        exampleCtx.getKeyManagement().clearTrustedSkis();
        exampleCtx.getKeyManagement().addTrustedSki(exampleSki, 32);
    }

    @Test
    public void test_registration() {
        exampleConn = new ShipConnectionImpl(false, 0, exampleCtx, basicListenerMock);
        assertEquals(State.CMI_INIT_START, exampleConn.getState());

        KeyManagement keyManagement = exampleCtx.getKeyManagement();

        assertThat(keyManagement.getTrustedSkis().get(exampleSki).isAuthenticated(), is(false));

        setUpHelloStateAndCallNext();
        assertThat(keyManagement.getTrustedSkis().get(exampleSki).isAuthenticated(), is(true));
    }

    @Test
    public void test_reconnection() {
        KeyManagement keyManagement = exampleCtx.getKeyManagement();

        keyManagement.setTrustedSkiAuthenticated(exampleSki);
        exampleConn = new ShipConnectionImpl(false, 0, exampleCtx, basicListenerMock);
        // see issue #61 in gitlab, for now leave the authenticated flag in, in case it is needed in the future
        assertEquals(State.CMI_INIT_START, exampleConn.getState());

        setUpHelloStateAndCallNext();
        assertThat(keyManagement.getTrustedSkis().get(exampleSki).isAuthenticated(), is(true));
    }

    private void setUpHelloStateAndCallNext() {
        // This is the reason the test is disabled:
        // the sanity checks in StateMachine prevent jumping directly to a
        // SME_HELLO_* state, we must get there in an orderly manner.
        // The replacement test should go via InstrumentedStateMachine (which does
        // allow directly jumping to any state). But this does not make much sense
        // with a global-state KeyManagement anyway.

        // SmeHelloState helloState = new SmeHelloState(exampleConn.getShipConnListener(),
        //     exampleCtx.getConfig(),
        //     exampleCtx.getLogPrefix()
        // );
        // helloState.next();
    }

}
