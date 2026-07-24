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

import java.net.Inet6Address;
import java.net.InetSocketAddress;

public class ShipUtilities {
    @SuppressWarnings("HardcodedFileSeparator")
    public static String beautify(InetSocketAddress socket) {
        String hostname = socket.getHostName();
        String ip = socket.getAddress().getHostAddress();
        int port = socket.getPort();

        boolean hasHostname = hostname != null && !ip.startsWith(hostname);
        boolean isIPv6 = socket.getAddress() instanceof Inet6Address;

        StringBuilder sb = new StringBuilder();

        if (hasHostname) {
            sb.append(hostname).append("/");
        }

        if (isIPv6) {
            sb.append("[").append(ip).append("]");
        } else {
            sb.append(ip);
        }

        sb.append(":").append(port);

        return sb.toString();
    }
}
