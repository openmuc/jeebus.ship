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

public enum DisconnectReason {
    REGULAR_END,
    /**
     * @deprecated since 2.3.0 and will be renamed to DOUBLE_CONNECTION in 3.0.0.
     */
    @Deprecated(since = "2.3.0",forRemoval = true)
    DUPLICATE_CONN,
    ERROR
}
