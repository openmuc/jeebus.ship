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
import org.openmuc.jeebus.ship.message.smeproth.ProtHError;
import org.openmuc.jeebus.ship.message.smeproth.ProtocolHandshakeMsg;
import org.openmuc.jeebus.ship.message.smeproth.ProtocolHandshakeTypeType;
import org.openmuc.jeebus.ship.state.machine.SpecifiedTimeout;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmeProtH {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmeProtH.class);

    private SmeProtH() {}

    public static void abort(ProtHError error, StateHandlerContext context) {
        context.stopTimeouts(SpecifiedTimeout.SME_PROTH_WAIT);
        context.sendMessage(ShipMessageFactory.parseSmeProtHErrorBody(error));
        context.closeConnection();
    }

    /**
     * Check that the given message is valid. If not, also logs why the message was
     * invalid.
     *
     * @param expectedType
     *     expected message type
     * @param msg
     *     message to check
     * @return whether the message is valid
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isValidMsg(
        ProtocolHandshakeTypeType expectedType,
        ProtocolHandshakeMsg msg
    ) {
        if (msg == null) {
            LOGGER.error("Not a protocol handshake message");
            return false;
        }
        if (msg.getHandshakeType() != expectedType) {
            LOGGER.error(
                "invalid message type: expected {}, got {}",
                expectedType,
                msg.getHandshakeType()
            );
            return false;
        }
        if (msg.getMajor() < 1 || msg.getMinor() < 0) {
            LOGGER.error("invalid version (negative number or does not support 1.0)");
            return false;
        }
        if (msg.getFormats().isEmpty()) {
            LOGGER.error("invalid message: no supported formats");
            return false;
        }
        return true;
    }

    /**
     * Check that the version &lt;major,minor&gt; is &le; to the version
     * &lt;maxMajor,maxMinor&gt;
     *
     * @param major
     *     queried major version
     * @param minor
     *     queried minor version
     * @param maxMajor
     *     max supported major version
     * @param maxMinor
     *     max supported minor version
     * @return whether the given version is supported
     */
    public static boolean versionSupported(
        int major,
        int minor,
        int maxMajor,
        int maxMinor
    ) {
        return major < maxMajor
            || major == maxMajor && minor <= maxMinor;
    }
}
