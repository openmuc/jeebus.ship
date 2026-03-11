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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openmuc.jeebus.ship.util.SHIPTestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Tag("complex")
public class DiscoveryTest {

    protected static final Logger log = LoggerFactory.getLogger(DiscoveryTest.class);

    @Test
    public void testDiscoveryNoIpAddress() throws IOException {
        doTestDiscovery(null, "011122334455", "011122334456", ".local1.");
    }

    @Test
    public void testDiscoveryLocalNetworkInterface() throws IOException {
        doTestDiscovery(
            SHIPTestUtil.getLocalHostLANAddress(),
            "021122334455",
            "021122334456",
            ".local2."
        );
    }

    @SuppressWarnings("HardcodedFileSeparator")
    private void doTestDiscovery(
        InetAddress address,
        String id1,
        String id2,
        String domain
    ) throws IOException {
        String serviceType = "_ship._tcp" + domain;

        String hostname = "EXAMPLEBRAND-EEB01M3EU-%s%s";

        ServiceRegistry serviceReg = new ServiceRegistry(
            address,
            String.format(hostname, id1, domain),
            serviceType,
            null
        );

        ServiceRegistry serviceReg2 = new ServiceRegistry(
            address,
            String.format(hostname, id2, domain),
            serviceType,
            null
        );

        TxtRecord txt = new TxtRecord(
            String.format(hostname, id1, ""),
            "/ship/",
            "1234AAAAFFFF1111CCCC3333EEEEDDDD99992222",
            true
        );

        ServiceInfo info = ServiceRegistry.createServiceInfo(
            serviceType,
            "Dishwasher ExampleCompany EEB01M3EU",
            SHIPTestUtil.getAvailablePort(0),
            txt
        );
        serviceReg.registerService(info);

        try {
            await().atMost(20, SECONDS).until(() -> serviceReg2.listServices().length == 1);

            ServiceInfo service = serviceReg2.listServices()[0];

            assertThat(service, is(info));
        }
        finally {
            serviceReg.shutdown();
            serviceReg2.shutdown();
        }
    }
}
