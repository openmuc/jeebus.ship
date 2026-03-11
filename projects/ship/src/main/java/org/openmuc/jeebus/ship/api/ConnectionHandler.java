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
     * called when a service is recognized and added
     *
     * @param ipAddr
     *     the corresponding ip address of the service that was added
     * @param ski
     *     the ski value of the service/node that was added
     */
    void serviceAdded(String ipAddr, String ski);

    /**
     * called when an added service was removed
     *
     * @param ipAddr
     *     the corresponding ip address of the service that was removed
     */
    void serviceRemoved(String ipAddr);

    /**
     * Called when the connection to a device reaches the state "Connection Data
     * Exchange". This is the State where non-SHIP-specific Messages (i.e. SPINE) are
     * exchanged.
     *
     * @param ipAddr
     *     the IP address of the connected device
     */
    void connectionDataExchangeEnabled(String ipAddr);

}
