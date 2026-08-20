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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CompletableFuture;

public class MainTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainTest.class);

    public static void main(String[] args) throws IOException {
        AtomicReference<byte[]> messageReceived = new AtomicReference<>(null);
        ConnectionHandler connHandler = new ConnectionHandler() {
            @Override
            public void onMessageReceived(
                byte[] fullMsg,
                byte[] payload,
                ShipConnectionInterface shipConn
            ) {
                messageReceived.set(payload);
                synchronized (MainTest.class) {
                    MainTest.class.notifyAll();
                }
            }

            @Override
            public void onDisconnect(
                DisconnectReason reason,
                ShipConnectionInterface shipConn
            ) {

            }

            @Override
            public void serviceAdded(ShipService service) {

            }

            @Override
            public void serviceRemoved(ShipService service) {

            }

            @Override
            public void connectionEstablished(ShipConnectionInterface connection) {

            }
        };

        ConnectionHandler connHandler2 = new ConnectionHandler() {
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

            }

            @Override
            public void serviceRemoved(ShipService service) {

            }

            @Override
            public void connectionEstablished(ShipConnectionInterface connection) {

            }

        };

        Instant begin = Instant.now();
        LOGGER.info("Beginning SHIP setup...");

        ShipConfig conf1 = ShipConfig.getBuilder()
            .withId("EXAMPLEBRAND-EEB01M3EU-001122334455")
            .withServerBindAddresses("localhost:8080")
            .withMDnsServiceInstance("Dishwasher ExampleCompany EEB01M3EU")
            .withCertificateDistinguishedName("CN=example name")
            .build();
        Ship ship = new Ship(conf1, connHandler);

        ship.setClientConnectedListener(ship::runConnectionDataPreparation);

        ShipConfig conf2 = ShipConfig.getBuilder()
            .withId("EXAMPLEBRAND-EEB01M3EU-001122334456")
            .withServerBindAddresses("localhost:8081")
            .withMDnsServiceInstance("Dishwasher ExampleCompany EEB01M4EU")
            .withCertificateDistinguishedName("CN=example name2")
            .build();
        Ship ship2 = new Ship(conf2, connHandler2);

        ship.addTrustedSki(ship2.getOwnSki());
        ship2.addTrustedSki(ship.getOwnSki());

        Instant openConnection = Instant.now();
        LOGGER.info("Opening connection...");

        CompletableFuture<ShipConnectionInterface> shipConnectionFuture = ship2.openConnection(
            new InetSocketAddress("localhost", 8080),
            "ship"
        );

        // Wait for the connection to be established
        ShipConnectionInterface shipConnInterface = shipConnectionFuture.join();

        Instant connectionReady = Instant.now();
        LOGGER.info("Connection ready, sending test message...");

        shipConnInterface.sendMsg(
            "{\"msg\":\"example payload\"}".getBytes(StandardCharsets.UTF_8)
        );

        byte[] message;
        while ((message = messageReceived.get()) == null) {
            synchronized (MainTest.class) {
                try {
                    MainTest.class.wait();
                } catch (InterruptedException e) {/*ignored*/}
            }
        }

        Instant messageArrived = Instant.now();
        LOGGER.info(
            "Got test message:\n\t{}",
            new String(message, StandardCharsets.UTF_8)
        );

        ship.close();
        ship2.close();

        LOGGER.info("SHIP close complete, timings:"
            + "\n\t- SHIP setup: {}"
            + "\n\t- Opening connection: {}"
            + "\n\t- Transmitting message: {}"
            + "\n\t- Shutting down: {}",
            Duration.between(begin, openConnection),
            Duration.between(openConnection, connectionReady),
            Duration.between(connectionReady, messageArrived),
            Duration.between(messageArrived, Instant.now())
        );

        // there appear to be some daemon threads left over, so: exit explicitly
        System.exit(0);
    }
}
