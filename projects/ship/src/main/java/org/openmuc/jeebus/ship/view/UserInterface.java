/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.view;

public interface UserInterface {

    boolean promptTrustCommunicationPartner(String prefix, String ski);

    /**
     * prompts user to accept/decline a prolongation request
     *
     * @param prefix
     *     server/client with port number
     * @return <code>true</code> if user accepted the prolongation request, else
     * <code>false</code>
     */
    boolean promptProlongationRequest(String prefix);

    boolean promptEnterRestrictedOk(String prefix);

    /**
     * asks user if user wants to send a PIN
     *
     * @param prefix
     *     server/client with port number
     * @return <code>true</code> if user wants to send a PIN, else <code>false</code>
     */
    boolean promptSendPin(String prefix);

    /**
     * prompts user to input a PIN. The returned PIN can then be sent to the
     * communication partner
     *
     * @param prefix
     *     server/client with port number
     * @return the input PIN
     */
    String promptInputPin(String prefix);

}
