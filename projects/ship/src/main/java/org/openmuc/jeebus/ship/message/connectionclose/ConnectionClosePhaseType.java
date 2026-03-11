/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message.connectionclose;

public enum ConnectionClosePhaseType {
    ANNOUNCE("announce"),
    CONFIRM("confirm");

    private String phase;

    ConnectionClosePhaseType(String phase) {
        this.phase = phase;
    }

    public static ConnectionClosePhaseType getByValue(String phaseString) {
        for (ConnectionClosePhaseType phase : ConnectionClosePhaseType.values()) {
            if (phase.phase.equals(phaseString)) {
                return phase;
            }
        }
        throw new IllegalArgumentException("no enum value found matching: "
            + phaseString);
    }

    @Override
    public String toString() {
        return phase;
    }
}
