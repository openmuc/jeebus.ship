/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node.service;

import org.junit.jupiter.api.Test;
import org.openmuc.jeebus.ship.api.ConfigBuilder;
import org.openmuc.jeebus.ship.node.ShipConfig;
import org.openmuc.jeebus.ship.util.ShipTestUtil;

import javax.jmdns.ServiceInfo;

import java.io.IOException;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceRegistryTest {
    private final String validId = "EXAMPLEBRAND-EEB01M3EU-001122334455";
    private final String validSki = "1234AAAAFFFF1111CCCC3333EEEEDDDD99992222";

    @Test
    public void testDiscoveryAnyAddress() throws IOException {
        doTestDiscovery(
            "011122334455",
            "011122334456",
            "local1.",
            "0.0.0.0:8081"
        );
    }

    @Test
    public void testDiscoveryLocalhost() throws IOException {
        doTestDiscovery(
            "021122334455",
            "021122334456",
            "local2.",
            "localhost:8080"
        );
    }

    private void doTestDiscovery(
        String id1,
        String id2,
        String domain,
        String... addresses
    ) throws IOException {
        String halfId = "EXAMPLEBRAND-EEB01M3EU-";

        String instance = "Dishwasher ExampleCompany EEB01M3EU";
        ConfigBuilder configBuilder = ShipConfig
            .getBuilder()
            .withServerBindAddresses(addresses)
            .withMDnsServiceInstance(instance)
            .withMDnsDomain(domain)
            .withCertificateDistinguishedName("CN=test")
            .withId(halfId + id1);

        ShipConfig config = configBuilder.build();

        ServiceRegistry serviceReg = new ServiceRegistry(
            config,
            null
        );

        serviceReg.initiateServices(
            validSki,
            config.getServerBindAddresses()
        );

        try (
            serviceReg;
            ServiceRegistry serviceReg2 = new ServiceRegistry(
                configBuilder.cloneWithoutSecrets().withId(halfId + id2).build(),
                null
        )) {
            await()
                .atMost(20, SECONDS)
                .until(() -> !serviceReg2.listServices().isEmpty());

            Optional<ServiceInfo> service = serviceReg2
                .listServices()
                .stream()
                .findAny();

            assertThat(
                service.orElseThrow().getName(),
                is(instance)
            );
        }
    }

    @Test
    public void testInvalidOptionalTxts() {
        int port = ShipTestUtil.getAvailablePort();

        ConfigBuilder config = ShipConfig
            .getBuilder()
            .withServerBindAddresses("localhost:" + port)
            .withMDnsServiceInstance("")
            .withCertificateDistinguishedName("CN=test")
            .withId(validId)
            .withBrand("")
            .withType("")
            .withModel("");

        ServiceRegistry registry = new ServiceRegistry(config.build(), null);

        TxtRecord txt = registry.createTxt(validSki);

        registry.validateTxt(txt);

        ServiceInfo serviceInfo = registry.createServiceInfo(
            port,
            txt
        );

        int txtRecordLen = serviceInfo.getTextBytes().length;

        String finalOptionalStr = "A".repeat(Math.max(0, (401 - txtRecordLen) / 2));

        assertThrows(
            IllegalArgumentException.class,
            () -> registry.validateTxt(new ServiceRegistry(
                config.cloneWithoutSecrets()
                    .withBrand(finalOptionalStr)
                    .withType(finalOptionalStr)
                    .build(),
                null
            ).createTxt(validSki)),
            "According to SHIP:7.3.2, the TXT record SHALL NOT exceed 400 "
                + "bytes in length."
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> registry.validateTxt(new ServiceRegistry(
                config.cloneWithoutSecrets()
                    .withModel(finalOptionalStr)
                    .withType(finalOptionalStr)
                    .build(),
                null
            ).createTxt(validSki)),
            "According to SHIP:7.3.2, the TXT record SHALL NOT exceed 400 "
                + "bytes in length."
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> registry.validateTxt(new ServiceRegistry(
                config.cloneWithoutSecrets()
                    .withModel(finalOptionalStr)
                    .withBrand(finalOptionalStr)
                    .build(),
                null
            ).createTxt(validSki)),
            "According to SHIP:7.3.2, the TXT record SHALL NOT exceed 400 "
                + "bytes in length."
        );
    }
}