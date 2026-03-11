/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node.websocket;

public class SkiManagementInfo {
    private int trustLevel;

    private boolean authenticated;

    public SkiManagementInfo(int trustLevel) {
        this.trustLevel = trustLevel;
    }

    public SkiManagementInfo(int trustLevel, boolean authenticated) {
        this(trustLevel);
        this.authenticated = authenticated;
    }

    public int getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(int trustLevel) {
        this.trustLevel = trustLevel;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
