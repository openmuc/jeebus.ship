/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node;

// TODO: consider cryptographic validation of SKI at the accessMethodsExchange
public class Identity {
    private final String id;
    private final String ski;

    public Identity(String id, String ski) {
        this.id = id;
        this.ski = ski;
    }

    public String getId() {
        return id;
    }

    public String getSki() {
        return ski;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Identity identity = (Identity) o;
        return id.equals(identity.getId()) && ski.equals(identity.getSki());
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + ski.hashCode();
        return result;
    }
}
