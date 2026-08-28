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
import java.util.concurrent.CompletableFuture;

public interface ShipInterface {

    /**
     * opens a connection to a device/server assuming {@code "ship"} for the WSS path
     *
     * @param ipAddr
     *     the socket address of the device/server to connect to. Example:
     *     "127.0.0.1:4059"
     * @return an object to represent this particular connection to another
     * device/server
     * @deprecated since 3.0.0 to be replaced by
     * {@link ShipInterface#openConnection(InetSocketAddress, String)}
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    ShipConnectionInterface openConnection(String ipAddr);

    /**
     * @return {@link ShipInterface#openConnection(InetSocketAddress, String, String,
     * String)} assuming {@code null} for expectedShipId and expectedSki. It is
     * recommended to use that method instead as it is more secure.
     */
    CompletableFuture<ShipConnectionInterface> openConnection(
        InetSocketAddress socket,
        String path
    );

    /**
     * Opens a connection to a SHIP server. Using this method is the safest way to
     * establish SHIP connections.
     *
     * @param socket
     *     the socket address of the target SHIP server to connect to. Use
     *     {@link ShipService#getInet4SocketAddress()} or
     *     {@link ShipService#getInet6SocketAddress()} to find the socket address of
     *     a resolved SHIP service.
     * @param path
     *     the WSS path to the SHIP server. Use {@link ShipService#getPath()} to find
     *     the path for a resolved SHIP service.
     * @param expectedShipId
     *     the SHIP ID we expect the remote device to have. If non-null, it is used
     *     to retrieve already exising connections to the device. If the device
     *     reports another SHIP ID than this, the connection is closed.
     * @param expectedSki
     *     the Subject Key Identifier we expect from the remote certificate. This is
     *     used to prevent spoofing attacks.
     *
     * @return a CompletableFuture that completes with an Interface to represent this
     * particular connection to another SHIP node, or fails if the connection attempt
     * was unsuccessful
     */
    CompletableFuture<ShipConnectionInterface> openConnection(
        InetSocketAddress socket,
        String path,
        String expectedShipId,
        String expectedSki
    );

    /**
     * sets a connection handler
     *
     * @param connectionHandler
     *     connectionHandler to handle all interactions with SHIP peers
     */
    void setConnectionHandler(ConnectionHandler connectionHandler);
}
