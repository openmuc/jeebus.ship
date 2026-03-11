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

import javax.annotation.Nullable;

public class CloseMsg {

    private ConnectionClosePhaseType phase;

    @Nullable
    private Integer maxTime;

    @Nullable
    private ConnectionCloseReasonType reason;

    public CloseMsg() {
    }

    public CloseMsg(ConnectionClosePhaseType phase) {
        this.phase = phase;
    }

    public CloseMsg(
        ConnectionClosePhaseType phase,
        @Nullable
        Integer maxTime,
        @Nullable
        ConnectionCloseReasonType reason
    ) {
        this.phase = phase;
        this.maxTime = maxTime;
        this.reason = reason;
    }

    public ConnectionClosePhaseType getPhase() {
        return phase;
    }

    public void setPhase(ConnectionClosePhaseType phase) {
        this.phase = phase;
    }

    public Integer getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(Integer maxTime) {
        this.maxTime = maxTime;
    }

    public ConnectionCloseReasonType getReason() {
        return reason;
    }

    public void setReason(ConnectionCloseReasonType reason) {
        this.reason = reason;
    }
}
