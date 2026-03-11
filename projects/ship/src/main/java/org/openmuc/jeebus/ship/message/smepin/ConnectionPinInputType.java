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

import org.openmuc.jeebus.ship.message.MessageUtility;

public class ConnectionPinInputType extends PinValue {
    private String pin;

    public ConnectionPinInputType() {
    }

    public ConnectionPinInputType(String pin) {
        this.pin = pin;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        if (MessageUtility.isValidPin(pin)) {
            this.pin = pin;
        }
        else {
            throw new IllegalArgumentException("pin is invalid");
        }
    }
}
