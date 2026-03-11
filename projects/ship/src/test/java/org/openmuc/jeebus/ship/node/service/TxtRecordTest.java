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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TxtRecordTest {

    private final int validTxtvers = 1;
    private final String validId = "EXAMPLEBRAND-EEB01M3EU-001122334455";
    private final String validPath = "/ship/";
    private final String validSki = "1234AAAAFFFF1111CCCC3333EEEEDDDD99992222";
    // following Strings are optional in creating TXT records
    private final String validBrand = "ExampleBrand";
    private final String validType = "Dishwasher";
    private final String validModel = "EEB01M3EU";

    @Test
    public void create_txt_record_with_valid_data_and_check_correct_textvers() {
        assertThat(
            new TxtRecord(validId, validPath, validSki, false).getTxtvers(),
            is(validTxtvers)
        );
        assertThat(
            new TxtRecord(validId, validPath, validSki, true).getTxtvers(),
            is(validTxtvers)
        );

        assertThat(
            new TxtRecord(
                validId,
                validPath,
                validSki,
                false,
                validBrand,
                validType,
                validModel
            ).getTxtvers(),
            is(validTxtvers)
        );
        assertThat(
            new TxtRecord(
                validId,
                validPath,
                validSki,
                true,
                validBrand,
                validType,
                validModel
            ).getTxtvers(),
            is(validTxtvers)
        );
    }

    private void checkExceptionForBothConstructors(
        String id,
        String path,
        String ski
    ) {
        assertThrows(
            IllegalArgumentException.class,
            () -> new TxtRecord(id, path, ski, false)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new TxtRecord(
                id,
                path,
                ski,
                true,
                validBrand,
                validType,
                validModel
            )
        );

    }

    @Test
    public void exceeding_length_id() {
        // this character is 2 bytes long
        checkExceptionForBothConstructors("€" + "a".repeat(62), validPath, validSki);
    }

    @Test
    public void invalid_paths() {
        checkExceptionForBothConstructors(validId, "", validSki);
        checkExceptionForBothConstructors(validId, ";", validSki);

        checkExceptionForBothConstructors(validId, "€" + "a".repeat(31), validSki);
    }

    @Test
    public void invalid_ski() {
        String invalidSki = "G".repeat(40);

        String shortSki = "1".repeat(39);

        String[] skis = { invalidSki, shortSki, "1".repeat(41) };
        for (String ski : skis) {
            checkExceptionForBothConstructors(validId, validPath, ski);
        }
    }

}
