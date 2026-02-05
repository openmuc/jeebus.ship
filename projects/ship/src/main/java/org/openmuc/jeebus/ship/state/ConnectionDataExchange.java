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
import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.cde.CDEMsg;
import org.openmuc.jeebus.ship.node.StaticConfiguration;
import org.openmuc.jeebus.ship.shipconnection.ShipConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ConnectionDataExchange {

    private final Logger log = LoggerFactory.getLogger(ConnectionDataExchange.class);

    private final ShipConnection connection;
    private final ConnectionHandler connHandler;

    private final Queue<CDEMsg> receivedMessageQueue;

    private final List<String> well_knownProtocolId;

    public ConnectionDataExchange(
        ShipConnection connection,
        ConnectionHandler connHandler
    ) {
        this.connection = connection;
        this.connHandler = connHandler;
        this.well_knownProtocolId = StaticConfiguration.getWell_knownProtocolId();
        receivedMessageQueue = new LinkedList<>();
    }

    public void processMsg(byte[] msg) {
        CDEMsg cdeMsg = null;
        try {
            cdeMsg = MessageUtility.preprocessCDEMsg(msg);
        }
        catch (IllegalArgumentException | JsonParseException e) {
            log.error(e.getMessage());
        }
        if (cdeMsg != null) {
            for (String protocolId : well_knownProtocolId) {
                if (cdeMsg.getHeader().equals(protocolId)) {
                    // only process message if received protocolId matches one of the well-known protocolIds
                    if (connHandler != null) {
                        connHandler.onMessageReceived(msg,
                            cdeMsg.getPayload().getBytes(StandardCharsets.UTF_8),
                            this.connection
                        );
                    }
                    else {
                        receivedMessageQueue.add(cdeMsg);
                    }
                    break;
                }
            }
        }
    }

    public void sendMsg(byte[] msg) {
        connection.sendRawMessage(msg);
    }

    public void sendCDE(CDEMsg cdeMsg) {
        sendMsg(ShipMessageFactory.parseCdeBody(cdeMsg));
    }

    public void sendCDE(String header, String payload) {
        sendCDE(new CDEMsg(header, payload));
    }

    public Queue<CDEMsg> getMsgQueue() {
        return receivedMessageQueue;
    }

    @Override
    public String toString() {
        return "Connection Data Exchange";
    }
}
