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
     * Called AT LEAST ONCE when a SHIP service is recognized and resolved. Use
     * {@link Ship#openConnection(InetSocketAddress, String)} to open a new client
     * connection to the SHIP server advertised in the service info.
     * <p>
     * This method might be called multiple times for the same device depending on
     * the network configuration.
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
     * Called when a new SHIP connection was sucessfully established. This means the
     * SHIP connection reached the state "Connection Data Exchange" and a proper
     * "Access methods" message containing their SHIP ID was received. It is strongly
     * recommended to only communicate with remote SHIP nodes once this method is
     * called.
     *
     * @param connection
     *     the connection interface to communicate with the remote SHIP node
     */
    void connectionEstablished(ShipConnectionInterface connection);

}
