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

public enum ProtHError {
    // RFU is a placeholder value
    RFU(0, "placeholder value, reserved for future use"),
    TIMEOUT(1, "Timeout"),
    UNEXPECTED_MESSAGE(2, "unexpected message"),
    SELECTION_MISMATCH(3, "selection mismatch");

    private int error;

    private String errorString;

    ProtHError(int error, String errorString) {
        this.error = error;
        this.errorString = errorString;
    }

    public static ProtHError getErrorFromByte(byte error) {
        return ProtHError.values()[error];
    }

    public static String getErrorType(int error) {
        return ProtHError.values()[error].getErrorString();
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
