/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message.smehello;

public enum ConnectionHelloPhaseType {
    PENDING("pending"),
    READY("ready"),
    ABORTED("aborted");

    private String type;

    ConnectionHelloPhaseType(String type) {
        this.type = type;
    }

    public static ConnectionHelloPhaseType getByValue(String typeString) {
        for (ConnectionHelloPhaseType type : ConnectionHelloPhaseType.values()) {
            if (type.type.equals(typeString)) {
                return type;
            }
        }
        throw new IllegalArgumentException("no enum value found matching: "
            + typeString);
    }

    @Override
    public String toString() {
        return type;
    }
}
