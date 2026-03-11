/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.machine.smehello;

import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloPhaseType;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloType;

import java.util.Objects;

public final class SmeExtraData {
    public final int prolongationRequestsAccepted;
    public final Integer lastWaiting;
    public final ConnectionHelloPhaseType partnerPhase;

    public SmeExtraData() {
        this(0, null, null);
    }

    public SmeExtraData(int prolongationRequestsAccepted,
        Integer lastWaiting, ConnectionHelloPhaseType phase
    ) {
        this.prolongationRequestsAccepted = prolongationRequestsAccepted;
        this.lastWaiting = lastWaiting;
        partnerPhase = phase;
    }

    public SmeExtraData withIncrementedRequests() {
        return new SmeExtraData(prolongationRequestsAccepted + 1, lastWaiting, partnerPhase);
    }

    public SmeExtraData withNewMessage(ConnectionHelloType msg) {
        Integer waiting = msg.getWaiting();
        ConnectionHelloPhaseType phase = msg.getPhase();
        return waiting == null && phase == this.partnerPhase ?
            this :
            new SmeExtraData(prolongationRequestsAccepted, msg.getWaiting(), phase);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SmeExtraData)) {
            return false;
        }
        SmeExtraData that = (SmeExtraData) o;
        return prolongationRequestsAccepted == that.prolongationRequestsAccepted;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(prolongationRequestsAccepted);
    }
}
