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
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

    public static final Comparator<InetAddress> SCOPED_ADDRESS_ORDER
        = (left, right) -> {

            int byteCompare = Arrays.compare(left.getAddress(), right.getAddress());

            if (byteCompare == 0
                && left instanceof Inet6Address && right instanceof Inet6Address
            ) {
                return Integer.compare(
                    ((Inet6Address) left).getScopeId(),
                    ((Inet6Address) right).getScopeId()
                );
            }
            return byteCompare;
        };

    @Nonnull
    public static Collector<InetAddress, ?, TreeSet<InetAddress>> toScopedAddressTreeSet() {
        return Collectors.toCollection(() -> new TreeSet<>(SCOPED_ADDRESS_ORDER));
    }
}
