/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.machine.smeproth;

import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.smeproth.ProtocolHandshakeTypeType;
import org.openmuc.jeebus.ship.node.StaticConfiguration;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;
import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;

@StateHandler.Handles(State.SME_PROT_H_CLIENT_INIT)
public class ClientInit implements StateHandler {

    @Override
    public void onEntered(State previous, StateHandlerContext context) {
        context.sendMessage(getAnnounceMaxMessage(context));
        context.startTimeout(SpecifiedTimeout.SME_PROTH_WAIT);
        context.transitionTo(State.SME_PROT_H_STATE_CLIENT_LISTEN_CHOICE);
    }

    public static byte[] getAnnounceMaxMessage(StateHandlerContext context) {
        StaticConfiguration config = context.getConfig();
        return ShipMessageFactory.parseSmeProtHBody(
            ProtocolHandshakeTypeType.ANNOUNCE_MAX,
            config.getMajor(),
            config.getMinor(),
            config.getSupportedFormats()
        );
    }
}
