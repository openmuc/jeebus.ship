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

import javax.annotation.Nonnull;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class ShipUtilities {
    public static String beautify(InetSocketAddress socket) {
        return getStringBuilder(socket.getAddress())
            .append(":")
            .append(socket.getPort())
            .toString();
    }

    public static String beautify(InetAddress address) {
        return getStringBuilder(address).toString();
    }

    @Nonnull
    private static StringBuilder getStringBuilder(InetAddress address) {
        String hostname = address.getHostName();
        String ip = address.getHostAddress();

        boolean hasHostname = hostname != null && !ip.startsWith(hostname);
        boolean isIPv6 = address instanceof Inet6Address;

        StringBuilder sb = new StringBuilder();

        if (hasHostname) {
            sb.append(hostname).append("/");
        }

        if (isIPv6) {
            sb.append("[").append(ip).append("]");
        } else {
            sb.append(ip);
        }
        return sb;
    }
}
