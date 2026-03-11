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

import java.util.List;

public class CliParameterBuilder {

    final String name;
    String description;
    boolean optional = true;

    public CliParameterBuilder(String name) {
        this.name = name;
    }

    public CliParameterBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public CliParameterBuilder setMandatory() {
        optional = false;
        return this;
    }

    public LongCliParameter buildLongParameter(
        String parameterName,
        long defaultValue
    ) {
        return new LongCliParameter(this, parameterName, defaultValue);
    }

    public LongCliParameter buildLongParameter(String parameterName) {
        return new LongCliParameter(this, parameterName);
    }

    public IntCliParameter buildIntParameter(
        String parameterName,
        int defaultValue
    ) {
        return new IntCliParameter(this, parameterName, defaultValue);
    }

    public IntCliParameter buildIntParameter(String parameterName) {
        return new IntCliParameter(this, parameterName);
    }

    public StringCliParameter buildStringParameter(
        String parameterName,
        String defaultValue
    ) {
        return new StringCliParameter(this, parameterName, defaultValue);
    }

    public StringCliParameter buildStringParameter(String parameterName) {
        return new StringCliParameter(this, parameterName);
    }

    public StringListCliParameter buildStringListParameter(String parameterName) {
        return new StringListCliParameter(this, parameterName);
    }

    public StringListCliParameter buildStringListParameter(
        String parameterName,
        List<String> defaultValue
    ) {
        return new StringListCliParameter(this, parameterName, defaultValue);
    }

    public FlagCliParameter buildFlagParameter() {
        return new FlagCliParameter(this);
    }

}
