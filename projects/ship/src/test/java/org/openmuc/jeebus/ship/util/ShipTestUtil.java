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

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ShipTestUtil {

    private static final int MIN_PORT_NUMBER = 2000;
    private static final int MAX_PORT_NUMBER = 65535;
    private static final int MAX_TRIES = 20;

    /**
     * @return the most likely interfaces for LAN connection (either wired or
     * wireless) among all interfaces of the local devices. Return the loopback IP
     * address if no LAN connection can be found
     * @throws UnknownHostException
     *     if no LAN connection or loopback IP can be found
     */
    public static InetAddress getLocalHostLanAddress()
        throws UnknownHostException, SocketException {

        try {
            InetAddress candidateAddress = null;
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
                 ifaces.hasMoreElements(); ) {
                NetworkInterface iface = ifaces.nextElement();
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses();
                     inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            return inetAddr;
                        }
                        candidateAddress = inetAddr;

                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }

            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException(
                    "The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        }
        catch (SocketException e) {
            SocketException socketException = new SocketException(
                "Failed to determine LAN address: " + e);
            socketException.initCause(e);
            throw socketException;
        }
    }

    /**
     *
     * @param lockedPorts pre-set ports you wish to use in the future, but currently not used.
     * @return free port as int
     */
    public static int getAvailablePort(int... lockedPorts) {
        for (int i = 0; i < MAX_TRIES; i++) {
            int port = new Random().nextInt((MAX_PORT_NUMBER - MIN_PORT_NUMBER) + 1)
                + MIN_PORT_NUMBER;
            try (ServerSocket ss = new ServerSocket(port)) {
                ss.setReuseAddress(true);
                if (ss.isBound() && notPlanedPort(port,lockedPorts)) {
                    return port;
                }
            }
            catch (IOException e) {
                // port is not available
            }
        }
        throw new RuntimeException("No available port found after "
            + MAX_TRIES
            + " tries");
    }

    private static boolean notPlanedPort(int port, int... lockedPorts) {
         for(int lockedPort:lockedPorts){
             if (lockedPort== port) {
                 return false;
             }
         }
         return true;
    }

    public static String getRandomShipId() {
        long randomNumber = Math.abs(new Random().nextLong()) % 1000000000000L;
        return String.format("EXAMPLEBRAND-EEB01M3EU-%012d", randomNumber);
    }

    @SafeVarargs
    public static Stream<Object[]> streamProduct(Supplier<Stream<Object>>... streams) {
        return streamProductInner(streams).get();
    }

    @SuppressWarnings("unchecked")
    private static Supplier<Stream<Object[]>> streamProductInner(Supplier<Stream<Object>>[] streams) {
        Supplier<Stream<Object[]>> first, second;
        switch (streams.length) {
            case 0:
                return () -> Stream.<Object[]>of(new Object[]{});
            case 1:
                return () -> streams[0].get().map(o -> new Object[]{o});
            case 2:
                first = () -> streams[0].get().map(o -> new Object[]{o});
                second = () -> streams[1].get().map(o -> new Object[]{o});
                break;
            default:
                int split = streams.length / 2;
                Supplier<Stream<Object>>[] firstArgs = new Supplier[split];
                Supplier<Stream<Object>>[] secondArgs = new Supplier[streams.length - split];
                System.arraycopy(streams, 0, firstArgs, 0, split);
                System.arraycopy(streams, split, secondArgs, 0, streams.length - split);
                first = streamProductInner(firstArgs);
                second = streamProductInner(secondArgs);
        }
        return () -> first.get().flatMap(half1 -> second.get().map(half2 -> {
            Object[] result = new Object[half1.length + half2.length];
            System.arraycopy(half1, 0, result, 0, half1.length);
            System.arraycopy(half2, 0, result, half1.length, half2.length);
            return result;
        }));
    }
}
