/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node;

import org.openmuc.jeebus.ship.message.smepin.PinStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Configuration {

    // trust level for user verified public keys
    public final static int USER_VERIFIED_TRUST_LEVEL = 32;
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
    private static final int MAJOR_VERSION = 1;
    private static final int MINOR_VERSION = 0;
    // the trust level for auto accept
    private static final int AUTO_ACCEPT_TRUST_LEVEL = 8;
    // timeout for receiving a pong frame in seconds
    private static final int PONG_RECEIVE_TIMEOUT = 10;
    // immutable list of supported protocols
    private static final List<String> PROTOCOLS = Collections.singletonList("TLSv1.2");
    // immutable list of supported cipher suites
    private static final List<String> CIPHERS = Collections.singletonList("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256");
    private static final List<String> WELL_KNOWN_PROTOCOL_ID = Collections.singletonList("ee1.0");


    private int autoAcceptWindow;
    private int cmiTimeoutVal;
    private int acceptProlongationRequests;
    // Wait-For-Ready-Timer initial
    private int T_hello_init;
    private final List<String> supportedFormats = new ArrayList<>();
    // pin requirement is dependent on device manufacturer
    // TODO: implement get pinState from device, for now set to none
    private PinStateType initialPinState = PinStateType.NONE;
    private int pinEntryPenaltyThirdAttempt = 15;
    private int pinEntryPenaltySixthAttempt = 90;

    /*
    parameter dump

    int Initial_TCP_retransmission_count = 2
    int Initial_TCP_retransmission_timeout = 1s
    int Maximum_TCP_retransmission_timeout = 120s
    int MTU(Maximum Transmission Unit) = 1500 (bytes)
    int Maximum_fragment_length = 512 (bytes)
    int Connection_Keepalive_ping_min_interval = 50s
    int Connection_Keepalive_pong_timeout = 10s
    int SKI_length = 20 (bytes) (40 digit hex string)
    int PIN_length = 8-16 (digit hex string)

    int user_trust_level_necessary_communication >= 8
    int user_trust_level_necessary_commissioning >= 32

    int PIN_entry_penalty (3rd invalid attempt) = default 15 (10-15)
    int PIN_entry_penalty (6th invalid attempt) = default 90 (60-90)
     */

    public Configuration() {
        try (
            InputStream configFile = Configuration.class
                .getClassLoader()
                .getResourceAsStream("config.properties")
        ) {
            Properties prop = new Properties();

            if (configFile == null) {
                LOGGER.error("Unable to find properties file.");
                return;
            }

            prop.load(configFile);

            autoAcceptWindow = Integer.parseInt(prop.getProperty(
                "maxAutoAcceptWindow",
                "60"
            ));
            if (autoAcceptWindow < 0 || autoAcceptWindow > 120) {
                LOGGER.warn(
                    "Value for maxAutoAcceptWindow not permitted. Read a value of "
                        + autoAcceptWindow
                        + " but must be from 0 to 120."
                        + " Setting to default value 60.");
                autoAcceptWindow = 60;
            }

            cmiTimeoutVal = Integer.parseInt(prop.getProperty("cmi.timeout", "30"));
            if (cmiTimeoutVal < 10 || cmiTimeoutVal > 30) {
                LOGGER.warn("Value for CmiTimeout not permitted. Read a value of "
                    + cmiTimeoutVal
                    + " but must be from 10 to 30."
                    + " Setting to default value 30.");
                cmiTimeoutVal = 30;
            }

            // T_hello_init=60-240, should be constant across connections
            T_hello_init = Integer.parseInt(prop.getProperty("hello.init", "120"));

            // can be replaced by comma separated string of formats in properties later
            supportedFormats.add("JSON-UTF8");

            pinEntryPenaltyThirdAttempt = Integer.parseInt(prop.getProperty(
                "pin.entryPenalty.third",
                "15"
            ));
            pinEntryPenaltySixthAttempt = Integer.parseInt(prop.getProperty(
                "pin.entryPenalty.sixth",
                "90"
            ));
        }
        catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static int getTrustLevel(boolean isAutoAccept) {
        return isAutoAccept ? AUTO_ACCEPT_TRUST_LEVEL : 0;
    }

    public static String[] getProtocolsAsArray() {
        return PROTOCOLS.toArray(new String[0]);
    }

    public static List<String> getProtocols() {
        return PROTOCOLS;
    }

    public static String[] getCipherAsArray() {
        return CIPHERS.toArray(new String[0]);
    }

    public static List<String> getCiphers() {
        return CIPHERS;
    }

    public static List<String> getWell_knownProtocolId() {
        return WELL_KNOWN_PROTOCOL_ID;
    }

    public int getAutoAcceptWindow() {
        return autoAcceptWindow;
    }

    public int getCmiTimeoutVal() {
        return cmiTimeoutVal;
    }

    public int getAcceptProlongationRequests() {
        return acceptProlongationRequests;
    }

    public int getT_hello_init() {
        return T_hello_init;
    }

    public int getMajor() {
        return MAJOR_VERSION;
    }

    public int getMinor() {
        return MINOR_VERSION;
    }

    // TODO alternative getter that returns a set
    public List<String> getSupportedFormats() {
        return supportedFormats;
    }

    public PinStateType getInitialPinState() {
        return initialPinState;
    }

    public void setInitialPinState(PinStateType pinState) {
        initialPinState = pinState;
    }

    public int getPinEntryPenaltyThirdAttempt() {
        return pinEntryPenaltyThirdAttempt;
    }

    public int getPinEntryPenaltySixthAttempt() {
        return pinEntryPenaltySixthAttempt;
    }

    public int getPongReceiveTimeout() {
        return PONG_RECEIVE_TIMEOUT;
    }
}
