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
import org.openmuc.jeebus.ship.util.SHIPTestUtil;

import javax.jmdns.ServiceInfo;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceRegistryTest {
    private final String validId = "EXAMPLEBRAND-EEB01M3EU-001122334455";
    private final String validPath = "/ship/";
    private final String validSki = "1234AAAAFFFF1111CCCC3333EEEEDDDD99992222";

    @Test
    public void invalid_optional_parameters() {
        TxtRecord txtRecord = new TxtRecord(validId, validPath, validSki, false);
        int port = SHIPTestUtil.getAvailablePort();
        ServiceInfo serviceInfo = ServiceRegistry.createServiceInfo("", "", port,
            txtRecord
        );

        int txtRecordLen = serviceInfo.getTextBytes().length;

        String finalOptionalStr = "A".repeat(Math.max(0, (401 - txtRecordLen) / 2));
        assertThrows(
            IllegalArgumentException.class,
            () -> ServiceRegistry.createServiceInfo("", "", port,
                new TxtRecord(
                    validId,
                    validPath,
                    validSki,
                    false,
                    finalOptionalStr,
                    finalOptionalStr,
                    null
                )
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ServiceRegistry.createServiceInfo("", "", port,
                new TxtRecord(
                    validId,
                    validPath,
                    validSki,
                    false,
                    null,
                    finalOptionalStr,
                    finalOptionalStr
                )
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ServiceRegistry.createServiceInfo("", "", port,
                new TxtRecord(
                    validId,
                    validPath,
                    validSki,
                    false,
                    finalOptionalStr,
                    null,
                    finalOptionalStr
                )
            )
        );
    }
}