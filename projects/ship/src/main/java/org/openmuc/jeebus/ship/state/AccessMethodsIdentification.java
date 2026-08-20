/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state;

import com.google.gson.JsonParseException;
import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.ami.AccessMethodsMsg;
import org.openmuc.jeebus.ship.message.ami.AccessMethodsRequestMsg;
import org.openmuc.jeebus.ship.shipconnection.ShipConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class AccessMethodsIdentification {
    private final static Logger LOGGER = LoggerFactory.getLogger(
        AccessMethodsIdentification.class);
    private final ShipConnection shipConn;

    private final String ownShipId;
    private AccessMethodsRequestMsg amrMsg;
    private AccessMethodsMsg amMsg;

    public AccessMethodsIdentification(
        ShipConnection shipConn,
        String ownShipId
    ) {
        this.shipConn = shipConn;
        this.ownShipId = ownShipId;
    }

    public void processMsg(byte[] msg) {
        try {
            if (new String(msg, StandardCharsets.UTF_8)
                .contains("accessMethodsRequest")
            ) {
                amrMsg = MessageUtility.preprocessAmrMsg(msg);
                shipConn.sendRawMessage(ShipMessageFactory.parseAmiBody(new AccessMethodsMsg(
                    ownShipId,
                    new AccessMethodsMsg.DnsSd_mDns(),
                    null
                )));
            }
            else {
                amMsg = MessageUtility.preprocessAmMsg(msg);
                shipConn.connectionEstablished();
            }
        }
        catch (IllegalArgumentException | JsonParseException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public void sendRequest() {
        shipConn.sendRawMessage(ShipMessageFactory.parseAmiBody(new AccessMethodsRequestMsg()));
    }

    public AccessMethodsRequestMsg getAmrMsg() {
        return amrMsg;
    }

    public void setAmrMsg(AccessMethodsRequestMsg amrMsg) {
        this.amrMsg = amrMsg;
    }

    public AccessMethodsMsg getAmMsg() {
        return amMsg;
    }

    public void setAmMsg(AccessMethodsMsg amMsg) {
        this.amMsg = amMsg;
    }

    @Override
    public String toString() {
        return "Access Method Identification";
    }
}
