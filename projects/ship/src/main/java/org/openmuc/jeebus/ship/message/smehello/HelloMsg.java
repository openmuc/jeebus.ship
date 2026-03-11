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

/**
 * wrapper class for ConnectionHelloType. Only needed for clarity and generating the
 * structure specified in the SHIP specification using Gson
 */
public class HelloMsg {

    private ConnectionHelloType connectionHello;

    public HelloMsg() {
    }

    public HelloMsg(ConnectionHelloType connectionHello) {
        this.connectionHello = connectionHello;
    }

    public ConnectionHelloType getConnectionHello() {
        return connectionHello;
    }

    public void setConnectionHello(ConnectionHelloType connectionHello) {
        this.connectionHello = connectionHello;
    }
}
