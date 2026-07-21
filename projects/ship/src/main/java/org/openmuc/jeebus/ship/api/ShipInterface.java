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

import java.net.URI;

public interface ShipInterface {

    /**
     * Opens a connection to a SHIP server. Use {@link ShipService#getInet4Uri()} or
     * {@link ShipService#getInet6Uri()} to get a correctly formatted URI for a
     * resolved SHIP service.
     *
     * @param serverUri
     *     the Universal Resource Identifier (URI) of the target SHIP server to
     *     connect to.
     * @return an Interface to represent this particular connection to another SHIP
     * node
     */
    ShipConnectionInterface openConnection(URI serverUri);

    /**
     * sets a connection handler
     *
     * @param connectionHandler
     *     connectionHandler to handle all interactions with SHIP peer
     */
    void setConnectionHandler(ConnectionHandler connectionHandler);
}
