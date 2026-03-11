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

    private Configuration config;

    private String logPrefix;

    private ConnectionHandler connHandler;

    public ShipNodeContext(Configuration config, String ownShipId) {
        this.config = config;
        this.ownShipId = ownShipId;
    }

    public ShipNodeContext(
        Configuration config,
        String logPrefix,
        ConnectionHandler connHandler,
        String ownShipId
    ) {
        this.config = config;
        this.logPrefix = logPrefix;
        this.connHandler = connHandler;
        this.ownShipId = ownShipId;
    }

    public Configuration getConfig() {
        return config;
    }

    public void setConfig(Configuration config) {
        this.config = config;
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
}
