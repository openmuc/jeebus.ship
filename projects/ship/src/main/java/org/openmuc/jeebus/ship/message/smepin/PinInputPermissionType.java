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

public enum PinInputPermissionType {
    BUSY("busy"),
    OK("ok");

    private String type;

    PinInputPermissionType(String type) {
        this.type = type;
    }

    public static PinInputPermissionType getByValue(String type) {
        for (PinInputPermissionType pipType : PinInputPermissionType.values()) {
            if (pipType.type.equals(type)) {
                return pipType;
            }
        }
        throw new IllegalArgumentException("no enum value found matching: " + type);
    }

    @Override
    public String toString() {
        return type;
    }
}
