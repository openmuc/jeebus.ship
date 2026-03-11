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

import org.openmuc.jeebus.ship.api.Ship;
import org.openmuc.jeebus.ship.api.ShipConnectionInterface;
import org.openmuc.jeebus.ship.cli.CliParser;

import java.util.List;

class ConsoleNodeCliParser {
    GenActionProcessor actionProcessor;
    CliParser cliParser;

    ConsoleNodeCliParser(Ship ship, List<ShipConnectionInterface> connList) {
        cliParser = new CliParser("ship-console-client", "SHIP client application");

        actionProcessor = new GenActionProcessor(ship, connList);

        actionProcessor.start();
    }

    /*public void createClient(String serviceId) throws CliParseException, IOException {
        switch (cliParser.getSelectedGroup()) {
        case "":
            ship.openConnection(serviceId);
            break;
        default:
            throw new IllegalArgumentException("Unknown connection type");
        }

        System.out.print("** Successfully connected to host: \n");
    }*/

    public void printUsage() {
        System.out.println(cliParser.getUsageString());
    }

}
