/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.openmuc.jeebus.ship.cli;

public final class CliParseException extends Exception {

    private static final long serialVersionUID = -5162894897245715377L;

    public CliParseException() {
        super();
    }

    public CliParseException(String s) {
        super(s);
    }

    public CliParseException(Throwable cause) {
        super(cause);
    }

    public CliParseException(String s, Throwable cause) {
        super(s, cause);
    }

}
