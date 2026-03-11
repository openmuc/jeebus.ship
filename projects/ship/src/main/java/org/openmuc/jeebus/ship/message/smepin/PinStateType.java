/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message.smepin;

public enum PinStateType {
    REQUIRED("required"),
    OPTIONAL("optional"),
    PIN_OK("pinOk"),
    NONE("none");

    private String type;

    PinStateType(String type) {
        this.type = type;
    }

    public static PinStateType getByValue(String type) {
        for (PinStateType psType : PinStateType.values()) {
            if (psType.type.equals(type)) {
                return psType;
            }
        }
        throw new IllegalArgumentException("no enum value found matching: " + type);
    }

    @Override
    public String toString() {
        return type;
    }
}
