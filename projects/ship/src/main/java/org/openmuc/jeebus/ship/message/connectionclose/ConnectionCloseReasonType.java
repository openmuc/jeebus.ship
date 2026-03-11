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

public enum ConnectionCloseReasonType {
    UNSPECIFIC("unspecific"),
    REMOVED_CONNECTION("removedConnection");

    private String reason;

    ConnectionCloseReasonType(String reason) {
        this.reason = reason;
    }

    public static ConnectionCloseReasonType getByValue(String reasonString) {
        for (ConnectionCloseReasonType reason : ConnectionCloseReasonType.values()) {
            if (reason.reason.equals(reasonString)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("no enum value found matching: "
            + reasonString);
    }

    @Override
    public String toString() {
        return reason;
    }
}
