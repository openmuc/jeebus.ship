/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.api.cert;

/**
 * Unchecked Exception thrown when authentication of remote SHIP devices fails.
 */
public class ShipAuthenticationException extends RuntimeException {
    public ShipAuthenticationException(String message) {
        super(message);
    }
    public ShipAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
