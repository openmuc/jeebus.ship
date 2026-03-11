/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message.smeproth;

public enum ProtocolHandshakeTypeType {
    ANNOUNCE_MAX("announceMax"),
    SELECT("select");

    private String type;

    ProtocolHandshakeTypeType(String type) {
        this.type = type;
    }

    public static ProtocolHandshakeTypeType getByValue(String type) {
        for (ProtocolHandshakeTypeType phtType : ProtocolHandshakeTypeType.values()) {
            if (phtType.type.equals(type)) {
                return phtType;
            }
        }
        throw new IllegalArgumentException("no enum value found matching: " + type);
    }

    @Override
    public String toString() {
        return type;
    }
}
