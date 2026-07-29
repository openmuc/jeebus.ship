/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.util;

import org.openmuc.jeebus.ship.node.ShipNodeContext;
import org.openmuc.jeebus.ship.shipconnection.ShipConnection;
import org.openmuc.jeebus.ship.view.UserInterface;

public class FakeShipConnection implements ShipConnection {
    private final boolean isServer;

    public FakeShipConnection(boolean isServer) {
        this.isServer = isServer;
    }

    @Override
    public boolean isServer() {
        return isServer;
    }

    @Override
    public void sendRawMessage(byte[] message) {

    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendMsg(byte[] msg) {

    }

    @Override
    public void closeImmediately() {

    }

    @Override
    public int getTrustLevel() {
        return 0;
    }

    @Override
    public void enableConnectionDataExchange() {
    }

    @Override
    public void setUserInterface(UserInterface userInterface) {
        throw new UnsupportedOperationException("fake ship connection has no user interface");
    }

    @Override
    public void initState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ShipNodeContext getShipNodeContext() {
        return null;
    }

    @Override
    public String getRemoteSki() {
        return "1234AAAAFFFF1111CCCC3333EEEEDDDD99992222";
    }

    @Override
    public String getRemoteAddress() {
        return "eebus-remote.example:12345";
    }

    @Override
    public void requestAccessMethods() {
        throw new UnsupportedOperationException();
    }
}
