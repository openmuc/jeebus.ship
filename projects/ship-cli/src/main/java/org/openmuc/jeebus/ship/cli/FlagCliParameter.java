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

public class FlagCliParameter extends CliParameter {

    FlagCliParameter(CliParameterBuilder builder) {
        super(builder);
    }

    @Override
    int appendSynopsis(StringBuilder sb) {
        int length = 0;
        if (optional) {
            sb.append("[");
            length++;
        }
        sb.append(name);
        length += name.length();
        if (optional) {
            sb.append("]");
            length++;
        }
        return length;
    }

    @Override
    void appendDescription(StringBuilder sb) {
        sb.append("\t").append(name).append("\n\t    ").append(description);
    }

    @Override
    int parse(String[] args, int i) throws CliParseException {
        selected = true;
        return 1;
    }

}
