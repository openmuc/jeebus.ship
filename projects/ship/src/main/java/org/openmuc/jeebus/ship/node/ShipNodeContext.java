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

import org.openmuc.jeebus.ship.api.ConnectionHandler;

public class ShipNodeContext {

    private final String ownShipId;

    private String expectedSki;
    private String expectedId;

    private final KeyManagement keyManagement;

    private String logPrefix;

    private ConnectionHandler connHandler;

    public ShipNodeContext(
        KeyManagement keyManagement,
        String ownShipId
    ) {
        this(keyManagement, ownShipId, null, null);
    }

    public ShipNodeContext(
        KeyManagement keyManagement,
        String ownShipId,
        String expectedId,
        String expectedSki
    ) {
        this.keyManagement = keyManagement;
        this.ownShipId = ownShipId;
        this.expectedId = expectedId;
        this.expectedSki = expectedSki;
    }

    public ShipNodeContext(
        String logPrefix,
        ConnectionHandler connHandler,
        String ownShipId,
        KeyManagement keyManagement
    ) {
        this.logPrefix = logPrefix;
        this.connHandler = connHandler;
        this.ownShipId = ownShipId;
        this.keyManagement = keyManagement;
    }

    public KeyManagement getKeyManagement() {
        return this.keyManagement;
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public void setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public ConnectionHandler getConnHandler() {
        return connHandler;
    }

    public void setConnHandler(ConnectionHandler connHandler) {
        this.connHandler = connHandler;
    }

    public String getOwnShipId() {
        return ownShipId;
    }

    public String getExpectedId() {
        return expectedId;
    }

    public String getExpectedSki() {
        return expectedSki;
    }
}
