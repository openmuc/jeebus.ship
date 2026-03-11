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

public class PinMsg {
    private PinValue pinValue;

    public PinMsg() {
    }

    public PinValue getPinValue() {
        return pinValue;
    }

    public void setPinValue(PinValue pinValue) {
        this.pinValue = pinValue;
    }
}
