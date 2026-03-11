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

public class DelegatingUserInterface implements UserInterface {
    private UserInterface delegate;

    public DelegatingUserInterface(UserInterface delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean promptTrustCommunicationPartner(String prefix, String ski) {
        return delegate.promptTrustCommunicationPartner(prefix, ski);
    }

    @Override
    public boolean promptProlongationRequest(String prefix) {
        return delegate.promptProlongationRequest(prefix);
    }

    @Override
    public boolean promptEnterRestrictedOk(String prefix) {
        return delegate.promptEnterRestrictedOk(prefix);
    }

    @Override
    public boolean promptSendPin(String prefix) {
        return delegate.promptSendPin(prefix);
    }

    @Override
    public String promptInputPin(String prefix) {
        return delegate.promptInputPin(prefix);
    }

    public UserInterface getDelegate() {
        return delegate;
    }

    public void setDelegate(UserInterface delegate) {
        this.delegate = delegate;
    }
}
