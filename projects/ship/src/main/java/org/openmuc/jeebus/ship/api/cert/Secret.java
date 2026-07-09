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

import java.util.Arrays;

public final class Secret implements AutoCloseable {

    private final char[] value;
    private boolean consumed = false;

    /**
     * @param value
     *     the value for the secret.
     * @implNote IMPORTANT: the given array is overwritten and cleared to increase
     * security.
     */
    public Secret(char[] value) {
        this.value = copy(value);
        clear(value);
    }

    public char[] consume() {
        if (consumed) {
            throw new IllegalStateException("Secret already consumed");
        }
        char[] copy = copy(value);
        close();
        return copy;
    }

    private static char[] copy(char[] what) {
        return Arrays.copyOf(what, what.length);
    }

    private static void clear(char[] what) {
        if (what != null) {
            Arrays.fill(what, '\0');
        }
    }

    @Override
    public void close() {
        clear(value);
        consumed = true;
    }
}