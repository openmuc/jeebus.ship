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

/**
 * users can interact with a specific client or server through this interface
 */
public interface ShipConnectionInterface extends AutoCloseable {
    @Override
    void close();

    /**
     * sends a message to the communication partner
     *
     * @param msg
     *     the message to send
     */
    void sendMsg(byte[] msg);

    /**
     * @return the current SKI of the communication partner
     */
    String getRemoteSki();

    /**
     * @return the IP address of the communication partner
     *
     * @deprecated since 2.3.0. The IP Address of remote nodes should be a
     * SHIP-internal detail, not part of the API.
     */
    @Deprecated(since = "2.3.0")
    String getRemoteAddress();

    /**
     * requests access methods from client, only works as server, an exception is
     * thrown otherwise
     */
    void requestAccessMethods();
}
