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

public abstract class CliParameter {

    final String name;
    final String description;
    final boolean optional;
    boolean selected;

    CliParameter(CliParameterBuilder builder) {
        name = builder.name;
        description = builder.description;
        optional = builder.optional;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the optional
     */
    public boolean isOptional() {
        return optional;
    }

    public boolean isSelected() {
        return selected;
    }

    abstract int parse(String[] args, int i) throws CliParseException;

    abstract int appendSynopsis(StringBuilder sb);

    abstract void appendDescription(StringBuilder sb);

}
