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
import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.smeproth.ProtHError;
import org.openmuc.jeebus.ship.message.smeproth.ProtocolHandshakeMsg;
import org.openmuc.jeebus.ship.message.smeproth.ProtocolHandshakeTypeType;
import org.openmuc.jeebus.ship.node.StaticConfiguration;
import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@StateHandler.Handles(State.SME_PROT_H_STATE_SERVER_LISTEN_PROPOSAL)
@StateHandler.UsesExtraData(ProtocolHandshakeMsg.class)
public class ServerListenProposal implements StateHandler {

    @Override
    public void onMessageReceived(StateHandlerContext context) {
        context.processMessage();
    }

    @Override
    public void processMessage(byte[] msg, StateHandlerContext context) {

        ProtocolHandshakeMsg proposal = null;
        try {
            proposal = MessageUtility.preprocessProtHMsg(msg);
        }
        catch (IllegalArgumentException e) {
            // fallthrough to abort
        }
        if (!SmeProtH.isValidMsg(ProtocolHandshakeTypeType.ANNOUNCE_MAX, proposal)) {
            SmeProtH.abort(ProtHError.UNEXPECTED_MESSAGE, context);
            return;
        }

        ProtocolHandshakeMsg reply = selectMsg(proposal, context);
        if (reply == null) { // couldn't agree on protocol
            return;
        }
        context.stopTimeouts(SpecifiedTimeout.SME_PROTH_WAIT);
        context.sendMessage(ShipMessageFactory.parseSmeProtHBody(reply));
        context.setExtraData(reply);
        context.startTimeout(SpecifiedTimeout.SME_PROTH_WAIT);
        context.transitionTo(State.SME_PROT_H_STATE_SERVER_LISTEN_CONFIRM);
    }

    private static ProtocolHandshakeMsg selectMsg(
        ProtocolHandshakeMsg proposal,
        StateHandlerContext context
    ) {
        StaticConfiguration config = context.getConfig();
        HashSet<String> supportedFormats
            = new HashSet<>(config.getSupportedFormats());
        Optional<String> foundFormat = proposal
            .getFormats()
            .stream()
            .filter(supportedFormats::contains)
            .findFirst();
        if (foundFormat.isEmpty()) {
            SmeProtH.abort(ProtHError.UNEXPECTED_MESSAGE, context);
            return null;
        }

        // ad hoc version comparison logic: compute the minimum, by lexicographic order.
        // we assume that each communication partner supports a contiguous range of versions,
        // e.g. if our version is <1, 3> and the communication partner announces <2, 0>
        // then we select <1, 3> and assume the communication partner can deal with this.
        int selectedMajor;
        int selectedMinor;
        if (config.getMajor() > proposal.getMajor()) {
            selectedMajor = proposal.getMajor();
            selectedMinor = proposal.getMinor();
        }
        else if (config.getMajor() < proposal.getMajor()) {
            selectedMajor = config.getMajor();
            selectedMinor = config.getMinor();
        }
        else { // ==
            selectedMajor = config.getMajor();
            selectedMinor = Math.min(config.getMinor(), proposal.getMinor());
        }
        return new ProtocolHandshakeMsg(
            ProtocolHandshakeTypeType.SELECT,
            selectedMajor,
            selectedMinor,
            List.of(foundFormat.get())
        );
    }
}
