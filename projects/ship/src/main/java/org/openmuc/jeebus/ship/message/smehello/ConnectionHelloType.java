/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message.smehello;

public class ConnectionHelloType {

    /**
     * sender's phase during "hello" process
     */
    private ConnectionHelloPhaseType phase;

    /**
     * remaining time (in millis) granted by sender
     */
    private Integer waiting;

    private Boolean prolongationRequest;

    /**
     * no-argument constructor for serialization/deserialization, stores information
     * of a SME_HELLO message
     */
    public ConnectionHelloType() {
    }

    /**
     * for convenience
     *
     * @param phase
     *     can not be null
     * @param waiting
     *     can be null
     * @param prolongationRequest
     *     can be null
     */
    public ConnectionHelloType(
        ConnectionHelloPhaseType phase,
        Integer waiting,
        Boolean prolongationRequest
    ) {
        if (phase == null) {
            throw new IllegalArgumentException("phase should not be null");
        }
        if (waiting != null && waiting < 0) {
            throw new IllegalArgumentException("waiting value should not be negative");
        }
        this.phase = phase;
        this.waiting = waiting;
        this.prolongationRequest = prolongationRequest;
    }

    public ConnectionHelloPhaseType getPhase() {
        return phase;
    }

    public void setPhase(ConnectionHelloPhaseType phase) {
        if (phase == null) {
            throw new IllegalArgumentException("phase should not be null");
        }
        this.phase = phase;
    }

    public Integer getWaiting() {
        return waiting;
    }

    public void setWaiting(Integer waiting) {
        this.waiting = waiting;
    }

    public Boolean getProlongationRequest() {
        return prolongationRequest;
    }

    public void setProlongationRequest(Boolean prolongationRequest) {
        this.prolongationRequest = prolongationRequest;
    }
}
