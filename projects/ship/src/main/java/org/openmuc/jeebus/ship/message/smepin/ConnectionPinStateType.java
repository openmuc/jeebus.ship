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

import javax.annotation.Nullable;

public class ConnectionPinStateType extends PinValue {
    private PinStateType pinState;
    private PinInputPermissionType inputPermission;

    public ConnectionPinStateType() {
    }

    public ConnectionPinStateType(
        PinStateType pinState,
        @Nullable
        PinInputPermissionType inputPermission
    ) {
        this.pinState = pinState;
        this.inputPermission = inputPermission;
    }

    public PinStateType getPinState() {
        return pinState;
    }

    public void setPinState(PinStateType pinState) {
        this.pinState = pinState;
    }

    public PinInputPermissionType getInputPermission() {
        return inputPermission;
    }

    public void setInputPermission(PinInputPermissionType inputPermission) {
        this.inputPermission = inputPermission;
    }
}
