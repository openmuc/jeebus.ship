/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.util;

import org.openmuc.jeebus.ship.view.UserInterface;

/**
 * Fake user interface meant for testing by overriding just one or a few methods of interest.
 * The method implementations throw {@link UnsupportedOperationException}, which makes
 * this class suitable for testing "the state handler does not access the user interface".
 * <p>
 * Implementation/usage example, where the test wants to keep track of how often
 * {@link #promptProlongationRequest(String)} was called:
 * <pre>
 * ThrowingUserInterface<Integer> ui = new ThrowingUserInterface<>() {
 *     { tracker = 0; }
 *     &commat;Override
 *     public boolean promptProlongationRequest(String prefix) {
 *         tracker++;
 *         return true;
 *     }
 * };
 * </pre>
 * @param <T> some arbitrary data tracked by the implementation.
 */
public class ThrowingUserInterface<T> implements UserInterface {
    public T tracker;
    @Override
    public boolean promptTrustCommunicationPartner(String prefix, String ski) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean promptProlongationRequest(String prefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean promptEnterRestrictedOk(String prefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean promptSendPin(String prefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String promptInputPin(String prefix) {
        throw new UnsupportedOperationException();
    }
}
