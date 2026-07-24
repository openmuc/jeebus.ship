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

// TODO: banish IP addresses from the API. Identities should be handled through
//  SHIP ID + SKI
public interface ConnectionHandler {
    /**
     * called when a message with payload was received
     *
     * @param fullMsg
     *     the received full message with headers
     * @param payload
     *     only the payload part of the received message
     * @param shipConn
     *     the connection that received the message
     */
    void onMessageReceived(
        byte[] fullMsg,
        byte[] payload,
        ShipConnectionInterface shipConn
    );

    /**
     * called when the connection is closed
     *
     * @param reason
     *     the reason why the connection was closed
     * @param shipConn
     *     the connection that was closed
     */
    void onDisconnect(DisconnectReason reason, ShipConnectionInterface shipConn);

    /**
     * Called when a SHIP service is recognized and resolved. Use
     * {@link Ship#openConnection(InetSocketAddress, String)} to open a new client
     * connection to the SHIP server advertised in the service info.
     *
     * @param service
     *     the complete SHIP mDNS Service Info with specialized access to its
     *     associated fields and TXT record values
     */
    void serviceAdded(ShipService service);

    /**
     * Called when a SHIP service is removed.
     *
     * @param service
     *     the SHIP service that was removed
     */
    void serviceRemoved(ShipService service);

    /**
     * Called when the connection to a device reaches the state "Connection Data
     * Exchange". This is the State where non-SHIP-specific Messages (i.e. SPINE) are
     * exchanged.
     *
     * @param ipAddr
     *     the IP address of the connected device
     *
     * @deprecated sinde 2.3.0. The IP Address of remote nodes should be a
     * SHIP-internal detail, not part of the API.
     */
    @Deprecated(since = "2.3.0")
    void connectionDataExchangeEnabled(String ipAddr);

}
