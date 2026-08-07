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

import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.api.Ship;
import org.openmuc.jeebus.ship.node.ShipConfig;
import org.openmuc.jeebus.ship.api.ShipConnectionInterface;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ExampleClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        /*
         implement your own ConnHandler, if used purely as client, connHandler
         can be left null
        */
        ConnectionHandler connHandler = null;

        // serviceId and serviceInstance should be unique in the network
        ShipConfig conf = ShipConfig.getBuilder()
            .withServerBindAddresses("localhost:2003")
            .withId("EXAMPLEBRAND-EEB01M4EU-001122334456")
            .withMDnsServiceInstance("Dishwasher ExampleCompany EEB01M4EU")
            .withCertificateDistinguishedName("CN=example name2")
            .withServerEnabled(false)
            .build();

        Ship ship = new Ship(conf, connHandler);

        /*
         Either authenticate the communication partner with auto accept mode or add
         their SKI to the trustedSkis list. Note that you have to know the SKI
         beforehand if authenticating before  opening the connection
        */
        // ship.addTrustedSki(peerSki);

        // replace String parameter with server IP as needed
        ShipConnectionInterface shipConnInterface = ship.openConnection(
            new InetSocketAddress("localhost", 2001),
            "ship"
        );

        /*
         The communication partner can also be authenticated after opening the
         connection.
        */
        String peerSki = shipConnInterface.getRemoteSki();
        ship.addTrustedSki(peerSki);

        byte[] exampleMsg
            = "{\"msg\":\"example payload\"}".getBytes(StandardCharsets.UTF_8);
        shipConnInterface.sendMsg(exampleMsg);

        Thread.sleep(1000);

        // connection can be closed with
        ship.close();
    }

}
