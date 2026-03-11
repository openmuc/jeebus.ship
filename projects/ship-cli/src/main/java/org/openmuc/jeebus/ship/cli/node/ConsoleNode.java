/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.cli.node;

import org.openmuc.jeebus.ship.api.*;
import org.openmuc.jeebus.ship.cli.*;
import org.openmuc.jeebus.ship.message.MessageUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConsoleNode {
    public static void main(String[] args) {
        CliParser cliParser = new CliParser(
            "ship-console-server",
            "SHIP console demo server application"
        );

        StringCliParameter inetAddr = new CliParameterBuilder("-ip").setDescription(
                "The IP address of the network interface that you want to bind the SHIP node to. Example: 192.168.1.2 ")
            .buildStringParameter("inetAddr", null);
        IntCliParameter port = new CliParameterBuilder("-p")
            .setDescription(
                "The port to listen on. SHIP servers usually listen on port 4059.")
            .buildIntParameter("port", 4059);

        StringCliParameter wssPath = new CliParameterBuilder("-path").setDescription(
                "The websocket-secure path for the initial server, example\"/ship/\"")
            .buildStringParameter("wss_path", "/");

        FlagCliParameter keepAlive = new CliParameterBuilder("-k")
            .setDescription("Use keepalive packets")
            .buildFlagParameter();

        StringCliParameter alias = new CliParameterBuilder("-a").setDescription(
                "Alias for the key pair in keystore which identifies the key entry")
            .setMandatory()
            .buildStringParameter("alias");

        StringCliParameter passphrase = new CliParameterBuilder("-store")
            .setDescription(
                "A passphrase that is used for the keystore")
            .setMandatory()
            .buildStringParameter("passphrase");

        StringCliParameter keyPairPass = new CliParameterBuilder("-pairPass")
            .setDescription(
                "A passphrase that is used for the key pair to be generated. Default value is the passphrase for the keystore")
            .buildStringParameter("keyPairPass", passphrase.getValue());

        StringCliParameter serviceId = new CliParameterBuilder("-id").setDescription(
                "The service id/service discovery host name, example: \"EXAMPLEBRAND-EEB01M3EU-001122334455\"")
            .setMandatory()
            .buildStringParameter("service_id");

        StringCliParameter serviceDomain = new CliParameterBuilder("-h")
            .setDescription(
                "The domain of the service types for the node to listen to. Default value is \"local.\"")
            .buildStringParameter("service_domain", "local.");

        StringCliParameter serviceInstance = new CliParameterBuilder("-i")
            .setDescription(
                "Service instance label for the initial server, example: \"Dishwasher ExampleCompany EEB01M3EU\"")
            .setMandatory()
            .buildStringParameter("service_instance");

        StringCliParameter dn = new CliParameterBuilder("-dn").setDescription(
                "X.509 Distinguished Name, example: \"CN=Test, L=London, C=GB\". For IoT devices, usually the DeviceID")
            .setMandatory()
            .buildStringParameter("dn");

        IntCliParameter days = new CliParameterBuilder("-days").setDescription(
                "How many days the certificate should be valid for. Default value is 365 days")
            .buildIntParameter("days", 365);

        cliParser.addParameters(
            Arrays.asList(inetAddr,
                port,
                wssPath,
                keepAlive,
                alias,
                passphrase,
                serviceId,
                serviceDomain,
                serviceInstance,
                dn,
                days
            ));
        try {
            cliParser.parseArguments(args);
        }
        catch (CliParseException e) {
            System.out.println(cliParser.getUsageString());
            return;
        }

        List<ShipConnectionInterface> connList = new ArrayList<>();

        // start up server
        ConnectionHandler connHandler = new ConnectionHandler() {
            @Override
            public void onMessageReceived(
                byte[] fullMsg,
                byte[] payload,
                ShipConnectionInterface shipConn
            ) {
                System.out.println("Received the following message from "
                    + shipConn.getRemoteAddress()
                    + ": "
                    + MessageUtility.parseShipMsgToString(fullMsg));
            }

            @Override
            public void onDisconnect(
                DisconnectReason reason,
                ShipConnectionInterface shipConn
            ) {
                connList.remove(shipConn);
            }

            @Override
            public void serviceAdded(String serviceId, String ski) {
            }

            @Override
            public void serviceRemoved(String serviceId) {
            }

            @Override
            public void connectionDataExchangeEnabled(String ipAddr) {

            }
        };
        ShipNodeConfiguration conf = new ShipNodeConfiguration(
            inetAddr.getValue(),
            port.getValue(),
            wssPath.getValue(),
            keepAlive.isSelected(),
            serviceId.getValue(),
            serviceDomain.getValue(),
            serviceInstance.getValue(),
            alias.getValue(),
            passphrase.getValue().toCharArray(),
            keyPairPass.getValue().toCharArray(),
            dn.getValue(),
            days.getValue()
        );
        Ship ship = new Ship(conf, connHandler);
        ship.setClientConnectedCB((connection) -> {
            if (!connList.contains(connection)) {
                connList.add(connection);
            }

            ship.runConnectionDataPreparation(connection);
        });

        new ConsoleNodeCliParser(ship, connList);
    }
}

