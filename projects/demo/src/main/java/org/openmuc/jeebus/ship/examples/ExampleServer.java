/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.examples;

import org.openmuc.jeebus.ship.api.*;
import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.node.ShipConfig;

import java.nio.charset.StandardCharsets;

public class ExampleServer {

    public static void main(String[] args) {

        // implement your own ConnHandler
        ConnectionHandler connHandler = new ConnectionHandler() {
            @Override
            public void onMessageReceived(
                byte[] fullMsg, byte[] payload, ShipConnectionInterface shipConn
            ) {
                System.out.println(MessageUtility.parseShipMsgToString(fullMsg));
            }

            @Override
            public void onDisconnect(
                DisconnectReason reason,
                ShipConnectionInterface shipConn
            ) {
                // this method can be used to keep track of server connections.
            }

            @Override
            public void serviceAdded(ShipService service) {

            }

            @Override
            public void serviceRemoved(ShipService service) {
                // do something
            }

            @Override
            public void clientConnected(ShipConnectionInterface connection) {
                /*
                 This is called when the connection to a device reaches the
                 state "Connection Data Exchange". This is the State where
                 non-SHIP-specific Messages (i.e. SPINE) are exchanged.
                */
                byte[] exampleMsg = "{\"msg\":\"greetings from the server\"}"
                    .getBytes(StandardCharsets.UTF_8);

                connection.sendMsg(exampleMsg);
            }
        };

        ShipConfig conf = ShipConfig.getBuilder()
            .withServerBindAddresses("localhost:2001")
            .withId("JEEBUS-EXAMPLE-SERVER-1")
            .withMDnsServiceInstance("Dishwasher ExampleCompany EEB01M3EU")
            .withCertificateDistinguishedName("CN=example name1")
            .withAutoAcceptEnabled(true)
            .build();

        new Ship(conf, connHandler);
    }
}