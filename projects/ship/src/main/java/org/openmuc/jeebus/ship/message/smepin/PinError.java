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

public enum PinError {
    // RFU is a placeholder value
    RFU(0, "placeholder value, reserved for future use"),
    WRONG_PIN(1, "wrong pin");

    private int error;

    private String errorString;

    PinError(int error, String errorString) {
        this.error = error;
        this.errorString = errorString;
    }

    public static PinError getByValue(int errorNum) {
        for (PinError pinError : PinError.values()) {
            if (pinError.error == errorNum) {
                return pinError;
            }
        }
        throw new IllegalArgumentException(
            "no pin error found matching error number: " + errorNum);
    }

    public static String getErrorType(int error) {
        return PinError.values()[error].getErrorString();
    }

    public String getErrorString() {
        return errorString;
    }

    public int getError() {
        return error;
    }

    public byte getErrorAsByte() {
        return (byte) error;
    }
}
