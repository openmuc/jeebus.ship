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

public final class ActionException extends Exception {

    private static final long serialVersionUID = 4806947065917148946L;

    public ActionException() {
        super();
    }

    public ActionException(String s) {
        super(s);
    }

    public ActionException(Throwable cause) {
        super(cause);
    }

    public ActionException(String s, Throwable cause) {
        super(s, cause);
    }

}
