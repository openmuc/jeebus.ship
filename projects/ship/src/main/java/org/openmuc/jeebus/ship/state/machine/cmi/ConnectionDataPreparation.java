/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.machine.cmi;

import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;

import static org.openmuc.jeebus.ship.node.ShipNodeParameters.MINIMAL_TRUST_LEVEL;

@StateHandler.Handles(State.CONNECTION_DATA_PREPARATION)
public class ConnectionDataPreparation implements StateHandler {
    @Override
    public void onEntered(State previous, StateHandlerContext context) {
        if (context.requireTrust(MINIMAL_TRUST_LEVEL)) {
            context.transitionTo(State.SME_HELLO_STATE_READY_INIT);
        } else {
            context.transitionTo(State.SME_HELLO_STATE_PENDING_INIT);
        }
    }
}
