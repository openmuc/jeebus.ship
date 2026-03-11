/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.api;

public interface ShipInterface {

    /**
     * opens a connection to a device/server
     *
     * @param ipAddr
     *     the IP address of the device/server to connect to. Example:
     *     "127.0.0.1:4059"
     * @return an object to represent this particular connection to another
     * device/server
     */
    ShipConnectionInterface openConnection(String ipAddr);

    /**
     * sets a connection handler
     *
     * @param connHandler
     *     connectionHandler to handle all interactions with SHIP peer
     */
    void setConnHandler(ConnectionHandler connHandler);

}
