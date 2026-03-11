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

abstract class ValueCliParameter extends CliParameter {

    String parameterName;

    ValueCliParameter(CliParameterBuilder builder, String parameterName) {
        super(builder);
        this.parameterName = parameterName;
    }

    @Override
    int appendSynopsis(StringBuilder sb) {
        int length = 0;
        if (optional) {
            sb.append("[");
            length++;
        }
        sb.append(name).append(" <").append(parameterName).append(">");
        length += (name.length() + 3 + parameterName.length());
        if (optional) {
            sb.append("]");
            length++;
        }
        return length;
    }

    @Override
    void appendDescription(StringBuilder sb) {
        sb
            .append("\t")
            .append(name)
            .append(" <")
            .append(parameterName)
            .append(">\n\t    ")
            .append(description);
    }
}
