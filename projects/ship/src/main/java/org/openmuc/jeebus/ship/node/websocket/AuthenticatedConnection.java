/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node.websocket;

import org.openmuc.jeebus.ship.shipconnection.BasicConnection;

/**
 * In addition to a {@link BasicConnection}, we also know who the communication
 * partner is.
 */
public interface AuthenticatedConnection extends BasicConnection {
    String getRemoteAddress();

    String getPeerSki();
}
