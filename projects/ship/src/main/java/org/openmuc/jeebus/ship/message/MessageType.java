/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message;

import java.util.Arrays;
import java.util.Optional;

public enum MessageType {
    INIT(0),
    CONTROL(1),
    DATA(2),
    END(3);

    private final byte value;

    MessageType(int value) {
        this.value = (byte) value;
    }

    public byte getValue() {
        return value;
    }

    public static Optional<MessageType> fromValue(byte value) {
        return Arrays
            .stream(values())
            .filter(type -> type.getValue() == value)
            .findFirst();
    }

    @Override
    public String toString() {
        return super.toString()+"("+getValue()+")";
    }
}