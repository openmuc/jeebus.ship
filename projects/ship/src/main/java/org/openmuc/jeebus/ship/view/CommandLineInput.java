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

import org.openmuc.jeebus.ship.message.MessageUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class CommandLineInput implements UserInterface {

    private static Logger log = LoggerFactory.getLogger(CommandLineInput.class);

    BufferedReader reader;
    BufferedWriter writer;

    public CommandLineInput() {
        reader = new BufferedReader(new InputStreamReader(System.in));
        writer = new BufferedWriter(new OutputStreamWriter(System.out));
    }

    private boolean genericYesNoPrompt(String context) {
        try {
            writer.write(context + " (N/y)\n");
            String input = reader.readLine();
            while (!(input == null
                || input.equalsIgnoreCase("y")
                || input.equalsIgnoreCase("n"))) {
                writer.write("Please answer with 'n' or 'y'\n");
                writer.flush();
                input = reader.readLine();
            }
            if (input != null && input.equalsIgnoreCase("y")) {
                return true;
            }
        }
        catch (IOException e) {
            log.error("encountered Exception while interacting with user: " + e);
        }
        return false;
    }

    public synchronized boolean promptTrustCommunicationPartner(
        String prefix,
        String ski
    ) {
        return genericYesNoPrompt(
            prefix
                + " received the public key "
                + ski
                + " from communication partner. Do you want to trust it?");
    }

    public synchronized boolean promptProlongationRequest(String prefix) {
        return genericYesNoPrompt(prefix
            + " received a prolongation request. Do you want to accept it?");
    }

    public synchronized boolean promptEnterRestrictedOk(String prefix) {
        return genericYesNoPrompt(
            prefix
                + ": communication partner pinState is 'optional'. Enter restricted ASK_OK state?");
    }

    public synchronized boolean promptSendPin(String prefix) {
        return genericYesNoPrompt(prefix + ": do you want to send a PIN?");
    }

    public synchronized String promptInputPin(String prefix) {
        try {
            writer.write("Please input a PIN now.");
            writer.flush();
            String input;
            while (!MessageUtility.isValidPin(input = reader.readLine())) {
                writer.write(input + " is not a valid PIN. Please try again.");
                writer.flush();
            }
            return input;
        }
        catch (IOException e) {
            log.error("encountered Exception while interacting with user: " + e);
        }
        throw new IllegalStateException("Unable to return a PIN.");
    }

}
