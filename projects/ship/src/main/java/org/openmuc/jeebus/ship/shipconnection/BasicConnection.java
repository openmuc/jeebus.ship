/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.shipconnection;

/**
 * The most basic form of connection: it can only send messages and close itself.
 */
public interface BasicConnection {
    void sendMsg(byte[] msg);

    void close();
}

