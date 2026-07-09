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

import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.message.smeproth.ProtHError;
import org.openmuc.jeebus.ship.message.smeproth.ProtocolHandshakeMsg;
import org.openmuc.jeebus.ship.message.smeproth.ProtocolHandshakeTypeType;
import org.openmuc.jeebus.ship.node.StaticConfiguration;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;
import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;

@StateHandler.Handles(State.SME_PROT_H_STATE_CLIENT_LISTEN_CHOICE)
public class ClientListenChoice implements StateHandler {
    @Override
    public void onMessageReceived(StateHandlerContext context) {
        context.processMessage();
    }

    @Override
    public void processMessage(byte[] msg, StateHandlerContext context) {
        StaticConfiguration config = context.getConfig();
        ProtocolHandshakeMsg choice
            = MessageUtility.preprocessProtHMsg(msg);
        if (!SmeProtH.isValidMsg(ProtocolHandshakeTypeType.SELECT, choice)) {
            SmeProtH.abort(ProtHError.SELECTION_MISMATCH, context);
            return;
        }
        if (choice.getFormats().size() == 1
            && config.getSupportedFormats().contains(choice.getFormats().get(0))
            && (choice.getMajor() < config.getMajor()
            || choice.getMajor() == config.getMajor()
            && choice.getMinor() <= config.getMinor())
        ) {
            context.sendMessage(msg);
            context.transitionTo(State.SME_PROT_H_STATE_CLIENT_OK);
        }
        else {
            SmeProtH.abort(ProtHError.SELECTION_MISMATCH, context);
        }
    }

    @Override
    public void onTimeoutExpired(SpecifiedTimeout kind, StateHandlerContext context) {
        if (kind == SpecifiedTimeout.SME_PROTH_WAIT) {
            context.transitionTo(State.SME_PROT_H_STATE_TIMEOUT);
        } else {
            StateHandler.super.onTimeoutExpired(kind, context);
        }
    }
}
