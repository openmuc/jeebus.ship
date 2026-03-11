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

public class IntCliParameter extends ValueCliParameter {

    Integer value;
    private Integer defaultValue = null;

    IntCliParameter(
        CliParameterBuilder builder,
        String parameterName,
        int defaultValue
    ) {
        super(builder, parameterName);
        this.defaultValue = defaultValue;
        value = defaultValue;
    }

    IntCliParameter(CliParameterBuilder builder, String parameterName) {
        super(builder, parameterName);
    }

    public int getValue() {
        return value;
    }

    @Override
    int parse(String[] args, int i) throws CliParseException {
        selected = true;

        if (args.length < (i + 2)) {
            throw new CliParseException("Parameter " + name + " has no value.");
        }

        try {
            value = Integer.decode(args[i + 1]);
        }
        catch (Exception e) {
            throw new CliParseException("Parameter value "
                + args[i + 1]
                + " cannot be converted to int.");
        }
        return 2;
    }

    @Override
    void appendDescription(StringBuilder sb) {
        super.appendDescription(sb);
        if (defaultValue != null) {
            sb.append(" Default is ").append(defaultValue).append(".");
        }
    }
}
