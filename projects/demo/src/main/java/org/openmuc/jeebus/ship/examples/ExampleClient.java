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
import org.openmuc.jeebus.ship.node.ShipConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ExampleClient {

    private static Ship ship;

    public static void main(String[] args) throws IOException, InterruptedException {
        /*
         implement your own ConnHandler, if used purely as client, connHandler
         can be left null
        */
        ConnectionHandler connHandler = new ConnectionHandler() {
            @Override
            public void onMessageReceived(
                byte[] fullMsg,
                byte[] payload,
                ShipConnectionInterface shipConn
            ) {

            }

            @Override
            public void onDisconnect(
                DisconnectReason reason,
                ShipConnectionInterface shipConn
            ) {

            }

            @Override
            public void serviceAdded(ShipService service) {
                // This may also happen during while establishing the SHIP connection
                ship.addTrustedSki(service.getSki());
            }

            @Override
            public void serviceRemoved(ShipService service) {

            }

            @Override
            public void connectionEstablished(ShipConnectionInterface connection) {

            }
        };

        // SHIP ID and serviceInstance should be unique in the network
        ShipConfig conf = ShipConfig.getBuilder()
            .withServerBindAddresses("localhost:2003")
            .withId("EXAMPLEBRAND-EEB01M4EU-001122334456")
            .withMDnsServiceInstance("Dishwasher ExampleCompany EEB01M4EU")
            .withCertificateDistinguishedName("CN=example name2")
            .withServerEnabled(false)
            .build();

        ship = new Ship(conf, connHandler);

        // replace String parameter with server IP as needed
        CompletableFuture<ShipConnectionInterface> shipConnInterfaceFuture = ship.openConnection(
            new InetSocketAddress("localhost", 2001),
            "ship"
        );

        // Wait for the connection to be established
        ShipConnectionInterface shipConnInterface = shipConnInterfaceFuture.join();

        byte[] exampleMsg
            = "{\"msg\":\"example payload\"}".getBytes(StandardCharsets.UTF_8);
        shipConnInterface.sendMsg(exampleMsg);

        Thread.sleep(1000);

        // connection can be closed with
        ship.close();
    }

}
