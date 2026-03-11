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
import org.openmuc.jeebus.ship.api.ShipConnectionInterface;
import org.openmuc.jeebus.ship.api.ShipNodeConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ExampleClient {

    public static void main(String[] args) throws IOException {
        char[] passphrase = "yourpassphrase".toCharArray();

        // replaceable with any free port
        int port = 2003;

        // replaceable with any String starting with '/'
        String wssPath = "/ship";

        String serviceDomain = "local.";

        /*
         implement your own ConnHandler, if used purely as client, connHandler
         can be left null
        */
        ConnectionHandler connHandler = null;

        // serviceId and serviceInstance should be unique in the network
        ShipNodeConfiguration conf = new ShipNodeConfiguration(
            port,
            wssPath,
            false,
            "EXAMPLEBRAND-EEB01M4EU-001122334456",
            serviceDomain,
            "Dishwasher ExampleCompany EEB01M4EU",
            "exampleAlias",
            passphrase,
            passphrase,
            "CN=example name2",
            3650
        );
        Ship ship = new Ship(conf, connHandler);

        /*
         Either authenticate the communication partner with auto accept mode or add
         their SKI to the trustedSkis list. Note that you have to know the SKI
         beforehand if authenticating before  opening the connection
        */
        // ship.addTrustedSki(peerSki);

        // auto accept mode will skip verification of public keys
        ship.setAutoAcceptMode();

        // replace String parameter with server IP as needed
        ShipConnectionInterface shipConnInterface = ship.openConnection(
            "localhost:2001");

        /*
         The communication partner can also be authenticated after opening the
         connection.
        */
        String peerSki = shipConnInterface.getRemoteSki();
        ship.addTrustedSki(peerSki);

        byte[] exampleMsg
            = "{\"msg\":\"example payload\"}".getBytes(StandardCharsets.UTF_8);
        shipConnInterface.sendMsg(exampleMsg);

        // connection can be closed with
        ship.shutDown();
    }

}
