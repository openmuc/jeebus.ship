/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openmuc.jeebus.ship.api.*;
import org.openmuc.jeebus.ship.api.cert.MemoryCertificateStorage;
import org.openmuc.jeebus.ship.node.ShipConfig;
import org.openmuc.jeebus.ship.shipconnection.ShipConnection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static net.obvj.junit.utils.matchers.ExceptionMatcher.throwsException;

@Execution(ExecutionMode.SAME_THREAD)
public class SystemTest {

    public static final ConfigBuilder COMMON_CONFIG = ShipConfig.getBuilder()
        .withServerBindAddresses("localhost:0")
        .withNetworkInterfaceScanInitialDelay(0)
        .withNetworkInterfaceScanInterval(1000)
        .withMDnsDomain(lookup().lookupClass().getSimpleName());

    public static final ConfigBuilder LEFT_CONFIG = COMMON_CONFIG.but()
        .withId("LEFT-ID")
        .withMDnsServiceInstance("left service")
        .withCertificateDistinguishedName("CN=left name")
        .withCertificateStorage(new MemoryCertificateStorage());

    public static final ConfigBuilder RIGHT_CONFIG = COMMON_CONFIG.but()
        .withId("RIGHT-ID")
        .withMDnsServiceInstance("right service")
        .withCertificateDistinguishedName("CN=right name")
        .withCertificateStorage(new MemoryCertificateStorage());

    public static final byte[] EXAMPLE_MESSAGE = "{\"msg\":\"example payload\"}"
        .getBytes(StandardCharsets.UTF_8);

    private Ship leftShip;
    private Ship rightShip;

    @Test
    public void testManualPreTrustedConnection() throws IOException {

        AtomicReference<byte[]> receivedCdeMessage = new AtomicReference<>(null);

        ConnectionHandler connHandler = new ConnectionHandler() {
            @Override
            public void onMessageReceived(
                byte[] fullMsg,
                byte[] payload,
                ShipConnectionInterface shipConn
            ) {
                receivedCdeMessage.set(payload);
            }

            @Override
            public void onDisconnect(
                DisconnectReason reason,
                ShipConnectionInterface shipConn
            ) {}

            @Override
            public void serviceAdded(ShipService service) {}

            @Override
            public void serviceRemoved(ShipService service) {}

            @Override
            public void clientConnected(ShipConnectionInterface connection) {}
        };

        ShipConfig leftConfig = LEFT_CONFIG.build();

        leftShip = new Ship(leftConfig, connHandler);

        rightShip = new Ship(
            RIGHT_CONFIG.but().withServerEnabled(false).build(),
            null
        );

        leftShip.addTrustedSki(rightShip.getOwnSki());
        rightShip.addTrustedSki(leftShip.getOwnSki());

        CompletableFuture<ShipConnectionInterface> future = rightShip.openConnection(
            leftShip.getServerSockets().stream().findAny().orElseThrow(),
            leftConfig.getWssPath(),
            leftConfig.getId(),
            leftShip.getOwnSki()
        );

        // Wait for the connection to be established
        ShipConnectionInterface connection = future.join();

        connection.sendMsg(EXAMPLE_MESSAGE);

        await().atMost(5, SECONDS).until(() -> receivedCdeMessage.get() != null);

        assertThat(
            receivedCdeMessage.get(),
            is(EXAMPLE_MESSAGE)
        );
    }

    @Test
    public void testServiceDiscoveryTrustedConnection() {

        AtomicReference<byte[]> receivedCdeMessage = new AtomicReference<>(null);

        AtomicReference<CompletableFuture<ShipConnectionInterface>> clientConnection
            = new AtomicReference<>(new CompletableFuture<>());

        ShipConfig leftConfig = LEFT_CONFIG.build();

        leftShip = new Ship(leftConfig, new ConnectionHandler() {
            @Override
            public void onMessageReceived(
                byte[] fullMsg,
                byte[] payload,
                ShipConnectionInterface shipConn
            ) {}

            @Override
            public void onDisconnect(
                DisconnectReason reason,
                ShipConnectionInterface shipConn
            ) {}

            @Override
            public void serviceAdded(ShipService service) {
                leftShip.addTrustedSki(service.getSki());

                clientConnection.set(leftShip.openConnection(
                    service
                        .getSocketAddresses()
                        .stream()
                        .findAny()
                        .orElseThrow(),
                    service.getPath(),
                    service.getShipId(),
                    service.getSki()
                ));
            }

            @Override
            public void serviceRemoved(ShipService service) {}

            @Override
            public void clientConnected(ShipConnectionInterface connection) {}
        });

        rightShip = new Ship(RIGHT_CONFIG.build(), new ConnectionHandler() {
            @Override
            public void onMessageReceived(
                byte[] fullMsg,
                byte[] payload,
                ShipConnectionInterface shipConn
            ) {
                receivedCdeMessage.set(payload);
            }

            @Override
            public void onDisconnect(
                DisconnectReason reason,
                ShipConnectionInterface shipConn
            ) {}

            @Override
            public void serviceAdded(ShipService service) {
                rightShip.addTrustedSki(service.getSki());
            }

            @Override
            public void serviceRemoved(ShipService service) {}

            @Override
            public void clientConnected(ShipConnectionInterface connection) {}
        });

        await().atMost(20, SECONDS).until(() -> clientConnection.get().isDone());

        clientConnection.get().join().sendMsg(EXAMPLE_MESSAGE);

        await().atMost(5, SECONDS).until(() -> receivedCdeMessage.get() != null);

        assertThat(
            receivedCdeMessage.get(),
            is(EXAMPLE_MESSAGE)
        );
    }

    @Test
    public void testManualToAutoAccept() {
        AtomicReference<byte[]> receivedCdeMessage = new AtomicReference<>(null);

        ShipConfig leftConfig = LEFT_CONFIG
            .but()
            .withAutoAcceptEnabled(true)
            .build();
        leftShip = new Ship(
            leftConfig,
            new ConnectionHandler() {
                @Override
                public void onMessageReceived(
                    byte[] fullMsg,
                    byte[] payload,
                    ShipConnectionInterface shipConn
                ) {
                    receivedCdeMessage.set(payload);
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
                public void clientConnected(ShipConnectionInterface connection) {

                }
            }
        );

        rightShip = new Ship(
            RIGHT_CONFIG.but().withTrustedSkis(leftShip.getOwnSki()).build(),
            null
        );

        ShipConnectionInterface connection = rightShip
            .openConnection(
                leftShip
                    .getServerSockets()
                    .stream()
                    .findAny()
                    .orElseThrow(), leftConfig.getWssPath()
            )
            .join();

        connection.sendMsg(EXAMPLE_MESSAGE);

        await().atMost(5, SECONDS).until(() -> receivedCdeMessage.get() != null);

        assertThat(
            receivedCdeMessage.get(),
            is(EXAMPLE_MESSAGE)
        );
    }

    @AfterEach
    public void tearDown() throws IOException {
        leftShip.close();
        rightShip.close();
    }
}
