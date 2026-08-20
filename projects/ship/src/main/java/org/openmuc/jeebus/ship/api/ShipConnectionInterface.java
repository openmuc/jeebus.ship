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
import java.net.URI;

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
     * @return the SKI of the communication partner
     */
    String getRemoteSki();

    /**
     * @return the unique SHIP ID of the communication partner
     */
    String getRemoteId();

    /**
     * @return the socket address of the communication partner
     */
    InetSocketAddress getRemoteAddress();

    /**
     * Try to parse the URI from the remote SHIP node's accessMethods message using
     * {@link URI#create}
     *
     * @return the URI from the remote SHIP node's accessMethods message.
     */
    URI getRemoteUri();

    /**
     * requests access methods and SHIP-ID
     * @deprecated since 3.0.0, as it happens automatically at the right stage.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    void requestAccessMethods();
}
