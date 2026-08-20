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
import java.util.ArrayList;
import java.util.List;

public class ExampleServer {

    private static Ship ship;

    public static void main(String[] args) {

        List<ShipConnectionInterface> serverConnections = new ArrayList<>();

        // implement your own ConnHandler
        ConnectionHandler connHandler = new ConnectionHandler() {
            @Override
            public void onMessageReceived(
                byte[] fullMsg, byte[] payload, ShipConnectionInterface shipConn
            ) {
                /*
                 This method can also be used like this to keep track of server
                 connections. Note that adding server connections only works in
                 auto-accept-mode as messages can not be exchanged with
                 unauthenticated partners. Alternatively use the clientConnectedListener
                 to authenticate the partner as well.
                */
                /*
                 if (!serverConnections.contains(shipConn)) {
                     serverConnections.add(shipConn);
                 }
                */
                System.out.println(MessageUtility.parseShipMsgToString(fullMsg));
            }

            @Override
            public void onDisconnect(
                DisconnectReason reason, ShipConnectionInterface shipConn
            ) {
                /*
                 this method can be used like this to keep track of server
                 connections, alternatively use the clientConnectedListener
                */
                serverConnections.remove(shipConn);
            }

            @Override
            public void serviceAdded(ShipService service) {
                /*
                 If you want you can authenticate any received SKI like this.
                 However, this is unsecure and not recommended.
                */
                ship.addTrustedSki(service.getSki());
            }

            @Override
            public void serviceRemoved(ShipService service) {
                // do something
            }

            @Override
            public void connectionEstablished(ShipConnectionInterface connection) {
                /*
                 This is called when the connection to a device reaches the
                 state "Connection Data Exchange". This is the State where
                 non-SHIP-specific Messages (i.e. SPINE) are exchanged.
                */
            }
        };

        /*
         keepAlive may be set as desired, but will only minimally influence
         behaviour as of now.
        */
        ShipConfig conf = ShipConfig.getBuilder()
            .withServerBindAddresses("localhost:2001")
            .withId("EXAMPLEBRAND-EEB01M3EU-001122334455")
            .withMDnsServiceInstance("Dishwasher ExampleCompany EEB01M3EU")
            .withCertificateDistinguishedName("CN=example name1")
            .build();
        ship = new Ship(conf, connHandler);
        // auto accept mode will skip verification of public keys
        // ship.setAutoAcceptMode();

        // additional SHIP servers may be created here as follows
        // TxtRecord txt = Ship.createTxtRecord(...);
        // ServiceInfo serviceInfo = Ship.createServiceInfo(..., txt);
        // ship.createServer(examplePort, exampleWssPath, false, serviceInfo);

        /*
         set a listener for when a client connects to the server, in this case
         connection data preparation will run once a client connects to the server
        */
        ship.setClientConnectedListener(ship::runConnectionDataPreparation);

        /*
         you can also configure ClientConnectedListener as follows to authenticate the
         partner and  send a message once connection data preparation finishes
        */
        ship.setClientConnectedListener((connection) -> {
            ship.addTrustedSki(connection.getRemoteSki());

            if (!serverConnections.contains(connection)) {
                serverConnections.add(connection);
            }

            ship.runConnectionDataPreparation(connection);

            byte[] exampleMsg = "{\"msg\":\"example payload\"}".getBytes(
                StandardCharsets.UTF_8);
            serverConnections.get(0).sendMsg(exampleMsg);
        });
    }

}