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
import org.openmuc.jeebus.ship.cli.Action;
import org.openmuc.jeebus.ship.cli.ActionException;
import org.openmuc.jeebus.ship.cli.ActionListener;
import org.openmuc.jeebus.ship.cli.ActionProcessor;

import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GenActionProcessor implements ActionListener {
    private static final String SCAN_FORMAT = "%-15s%-30s%-40s%-25s%n";

    private static final String CONN_LIST_FORMAT = "%-30s%s%n";

    private static final String WRITE_ACTION_KEY = "w";
    private static final String SCAN_NODES_ACTION_KEY = "s";
    private static final String CONNECT_ACTION_KEY = "c";
    private static final String DISCONNECT_ACTION_KEY = "d";
    private static final String AUTH_NODE_ACTION_KEY = "a";
    private static final String AUTO_ACCEPT_ACTION_KEY = "aa";

    private ActionProcessor actionProcessor;

    private Ship ship;
    private List<ShipConnectionInterface> connList;
    private ServiceInfo[] services;

    public GenActionProcessor(Ship ship, List<ShipConnectionInterface> connList) {
        this.ship = ship;
        this.connList = connList;
    }

    public void start() {
        actionProcessor = new ActionProcessor(this);
        actionProcessor.addAction(new Action(WRITE_ACTION_KEY, "write"));
        actionProcessor.addAction(new Action(
            SCAN_NODES_ACTION_KEY,
            "scan for SHIP nodes"
        ));
        actionProcessor.addAction(new Action(
            CONNECT_ACTION_KEY,
            "scan for SHIP nodes and connect to a node"
        ));
        actionProcessor.addAction(new Action(
            DISCONNECT_ACTION_KEY,
            "close a connection"
        ));
        actionProcessor.addAction(new Action(
            AUTH_NODE_ACTION_KEY,
            "authenticate another SHIP node by SKI"
        ));
        actionProcessor.addAction(new Action(
            AUTO_ACCEPT_ACTION_KEY,
            "enable auto-accept mode"
        ));

        actionProcessor.start();
    }

    @Override
    public void actionCalled(String actionKey) throws ActionException {
        try {
            switch (actionKey) {
                case WRITE_ACTION_KEY:
                    try {
                        processWrite();
                    }
                    catch (IllegalArgumentException e) {
                        System.err.println(e.getMessage());
                    }
                    break;
                case SCAN_NODES_ACTION_KEY:
                    System.out.println("** Scan nodes started...");
                    processScanNodes();
                    break;
                case CONNECT_ACTION_KEY:
                    System.out.println("** Scan nodes started...");
                    processScanNodes();
                    processConnect();
                    break;
                case DISCONNECT_ACTION_KEY:
                    processDisconnect();
                    break;
                case AUTH_NODE_ACTION_KEY:
                    System.out.println("** Scan nodes started...");
                    processScanNodes();
                    processAuth();
                    break;
                case AUTO_ACCEPT_ACTION_KEY:
                    System.out.println("** Entering auto-accept mode...");
                    ship.setAutoAcceptMode();
                    break;
                default:
                    break;
            }
        }
        catch (Exception e) {
            throw new ActionException(e);
        }
    }

    @Override
    public void quit() {
        System.out.println("** Closing node");
        close();
    }

    public void processWrite() throws IOException {
        System.out.println("** Available connections:");
        System.out.printf(CONN_LIST_FORMAT, "Index", "Remote address");
        printConnections();
        System.out.println(
            "** Enter: Index of the socket to be used to send the message");
        System.out.println("** Enter 'c' to cancel");

        String index;
        int indexInt = 0;
        do {
            index = actionProcessor.getReader().readLine();
            try {
                if (index.equalsIgnoreCase("c")) {
                    return;
                }
                indexInt = Integer.parseInt(index);
                if (indexInt < 0 || indexInt >= connList.size()) {
                    throw new NumberFormatException("number invalid");
                }
            }
            catch (NumberFormatException nfe) {
                System.out.println("Please input a valid number");
                index = null;
            }
        } while (index == null);

        System.out.println("** Enter: the message to send");

        String inputData;

        do {
            inputData = actionProcessor.getReader().readLine();
        } while (inputData == null);

        connList.get(indexInt).sendMsg(inputData.getBytes(UTF_8));
    }

    private void processDisconnect() throws IOException {
        System.out.println("** Available connections:");
        System.out.printf(CONN_LIST_FORMAT, "Index", "Remote address");
        printConnections();

        System.out.println("** Enter: Index of the socket to close");
        System.out.println("** Enter 'c' to cancel");
        String index;
        int indexInt = 0;
        do {
            index = actionProcessor.getReader().readLine();
            try {
                if (index.equalsIgnoreCase("c")) {
                    return;
                }
                indexInt = Integer.parseInt(index);
                if (indexInt < 0 || indexInt >= connList.size()) {
                    throw new NumberFormatException("number invalid");
                }
            }
            catch (NumberFormatException nfe) {
                System.out.println("Please input a valid number");
                index = null;
            }
        } while (index == null);

        connList.get(indexInt).close();
    }

    private void printConnections() {
        int index = 0;
        for (ShipConnectionInterface conn : connList) {
            System.out.printf(CONN_LIST_FORMAT, index, conn.getRemoteAddress());
            index++;
        }
    }

    public void close() {
        try {
            ship.shutDown();
        }
        catch (IOException e) {
            System.err.println("Error shutting down SHIP node: " + e.getMessage());
            e.printStackTrace();
        }
        actionProcessor.close();
    }

    public void processScanNodes() {
        System.out.println("Scanned addresses:");
        System.out.printf(
            SCAN_FORMAT,
            "Index",
            "Address",
            "Service name",
            "Description"
        );

        printNodes();
    }

    private void printNodes() {
        services = ship.getServices();

        int index = 0;
        for (ServiceInfo service : services) {
            String address = service.getURLs()[0];
            String name = service.getName();
            String desc = service.getNiceTextString();
            System.out.printf(SCAN_FORMAT, index, address, name, desc);
            index++;
        }
    }

    public void processAuth() throws IOException {
        System.out.println("** Enter: Index of the node to authenticate");
        System.out.println("** Enter 'c' to cancel");

        String index;
        int indexInt = 0;
        do {
            index = actionProcessor.getReader().readLine();
            if (index.equalsIgnoreCase("c")) {
                return;
            }
            try {
                indexInt = Integer.parseInt(index);
                if (indexInt < 0 || indexInt > services.length) {
                    throw new NumberFormatException("number invalid");
                }
            }
            catch (NumberFormatException nfe) {
                System.out.println("Please input a valid number");
                index = null;
            }
        } while (index == null);

        ServiceInfo selectedService = services[indexInt];
        String ski = selectedService.getPropertyString("ski");
        ship.addTrustedSki(ski);
    }

    public void processConnect() throws IOException {
        System.out.println("** Enter: Index of the node to connect to");
        System.out.println("** Enter 'c' to cancel");

        String index;
        int indexInt = 0;
        do {
            index = actionProcessor.getReader().readLine();
            if (index.equalsIgnoreCase("c")) {
                return;
            }
            try {
                indexInt = Integer.parseInt(index);
                if (indexInt < 0 || indexInt > services.length) {
                    throw new NumberFormatException("number invalid");
                }
            }
            catch (NumberFormatException nfe) {
                System.out.println("Please input a valid number");
                index = null;
            }
        } while (index == null);

        ServiceInfo selectedService = services[indexInt];
        String ipAddr = selectedService.getURLs("")[0].substring(3);
        ShipConnectionInterface shipConn = ship.openConnection(ipAddr);
        connList.add(shipConn);
    }
}
