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

import java.net.InetSocketAddress;

public interface ShipInterface {

    /**
     * opens a connection to a device/server
     *
     * @param ipAddr
     *     the IP address of the device/server to connect to. Example:
     *     "127.0.0.1:4059"
     * @return an object to represent this particular connection to another
     * device/server
     * @deprecated since 3.0.0 to be replaced by
     * {@link ShipInterface#openConnection(InetSocketAddress, String)}
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    ShipConnectionInterface openConnection(String ipAddr);

    /**
     * Opens a connection to a SHIP server.
     *
     * @param socket
     *     the socket address of the target SHIP server to connect to. Use
     *     {@link ShipService#getInet4SocketAddress()} or
     *     {@link ShipService#getInet6SocketAddress()} to find the socket address of
     *     a resolved SHIP service.
     * @param path
     *     the WSS path to the SHIP server. Use {@link ShipService#getPath()} to find
     *     the path for a resolved SHIP service.
     * @return an Interface to represent this particular connection to another SHIP
     * node
     */
    ShipConnectionInterface openConnection(
        InetSocketAddress socket,
        String path
    );

    /**
     * sets a connection handler
     *
     * @param connectionHandler
     *     connectionHandler to handle all interactions with SHIP peers
     */
    void setConnectionHandler(ConnectionHandler connectionHandler);
}
