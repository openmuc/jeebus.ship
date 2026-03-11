/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openmuc.jeebus.ship.message.ami.AMRTypeAdapter;
import org.openmuc.jeebus.ship.message.ami.AMTypeAdapter;
import org.openmuc.jeebus.ship.message.ami.AccessMethodsMsg;
import org.openmuc.jeebus.ship.message.ami.AccessMethodsRequestMsg;
import org.openmuc.jeebus.ship.message.cde.CDEMsg;
import org.openmuc.jeebus.ship.message.cde.CDETypeAdapter;
import org.openmuc.jeebus.ship.message.connectionclose.CloseMsg;
import org.openmuc.jeebus.ship.message.connectionclose.ConnectionClosePhaseType;
import org.openmuc.jeebus.ship.message.connectionclose.ConnectionCloseReasonType;
import org.openmuc.jeebus.ship.message.connectionclose.ConnectionCloseTypeAdapter;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloPhaseType;
import org.openmuc.jeebus.ship.message.smehello.ConnectionHelloType;
import org.openmuc.jeebus.ship.message.smehello.HelloMsg;
import org.openmuc.jeebus.ship.message.smehello.HelloTypeAdapter;
import org.openmuc.jeebus.ship.message.smepin.*;
import org.openmuc.jeebus.ship.message.smeproth.*;

import javax.annotation.Nullable;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ShipMessageFactory {

    private ShipMessageFactory() {
    }

    public static byte[] constructShipMsg(int messageType, byte[] messageValue) {
        byte msgType = (byte) messageType;
        byte[] msg = new byte[1 + messageValue.length];
        msg[0] = msgType;
        System.arraycopy(messageValue, 0, msg, 1, messageValue.length);
        return msg;
    }

    public static byte[] cmiMsg(byte cmiHead, byte[] cmiRemainder) {
        byte messageType = (byte) 0;
        byte[] msg = new byte[2 + cmiRemainder.length];
        msg[0] = messageType;
        msg[1] = cmiHead;
        System.arraycopy(cmiRemainder, 0, msg, 2, cmiRemainder.length);
        return msg;
    }

    public static byte[] smeHelloMsg(byte[] smeHelloValue) {
        return constructShipMsg(1, smeHelloValue);
    }

    public static byte[] smeProtMsg(byte[] smeProtHandshakeValue) {
        return constructShipMsg(1, smeProtHandshakeValue);
    }

    public static byte[] smePinMsg(byte[] smePinValue) {
        return constructShipMsg(1, smePinValue);
    }

    public static byte[] amiMsg(byte[] amiValue) {
        return constructShipMsg(1, amiValue);
    }

    public static byte[] cdeMsg(byte[] cdeValue) {
        return constructShipMsg(2, cdeValue);
    }

    public static byte[] connectionCloseMsg(byte[] connCloseMsg) {
        return constructShipMsg(3, connCloseMsg);
    }

    /**
     * constructs and returns a SME_HELLO message with the given parameters.
     *
     * @param phaseType
     *     The sender's phase during the "hello" process (enumeration: pending,
     *     ready, aborted)
     * @param waitForReady
     *     remaining wait times granted
     * @param prolongationReq
     *     Request to prolong the remaining wait time
     * @return the complete SME_HELLO message as a byte array
     */
    public static byte[] parseSmeHelloBody(
        ConnectionHelloPhaseType phaseType, Integer waitForReady,
        Boolean prolongationReq
    ) {
        if (phaseType == null) {
            throw new IllegalArgumentException("phaseType cannot be empty");
        }
        ConnectionHelloType parameters = new ConnectionHelloType(
            phaseType,
            waitForReady,
            prolongationReq
        );
        HelloMsg msg = new HelloMsg(parameters);
        return parseSmeHelloBody(msg);
    }

    /**
     * same as the other parseSmeHelloBody but with SmeHello as parameter
     *
     * @param s
     *     Hello message
     * @return the complete SME_HELLO message as a byte array
     */
    public static byte[] parseSmeHelloBody(HelloMsg s) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(HelloMsg.class, new HelloTypeAdapter())
            .create();
        return smeHelloMsg(gson.toJson(s).getBytes(UTF_8));
    }

    /**
     * constructs and returns a SME_PROT_H message with the given parameters
     *
     * @param handshakeType
     *     The kind of the handshake information (enumeration: announceMax, select).
     * @param major
     *     Version information: Major version part
     * @param minor
     *     Version information: Minor version part.
     * @param formats
     *     Protocol format(s)
     * @return the complete SME_PROT_H message as a byte array
     */
    public static byte[] parseSmeProtHBody(
        ProtocolHandshakeTypeType handshakeType, int major, int minor,
        List<String> formats
    ) {
        if (formats.isEmpty()) {
            throw new IllegalArgumentException("Formats should not be empty");
        }
        ProtocolHandshakeMsg ph = new ProtocolHandshakeMsg();
        ph.setHandshakeType(handshakeType);
        ph.setMajor(major);
        ph.setMinor(minor);
        ph.setFormats(formats);

        return parseSmeProtHBody(ph);
    }

    /**
     * same as the other parseSmeProtHBody method but with ProtocolHandshake Object
     * as parameter for ease of use
     *
     * @param ph
     *     the object to parse
     * @return the complete SME_PROT_H message as a byte array
     */
    public static byte[] parseSmeProtHBody(ProtocolHandshakeMsg ph) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(ProtocolHandshakeMsg.class, new ProtHTypeAdapter())
            .create();
        return smeProtMsg(gson.toJson(ph).getBytes(UTF_8));
    }

    public static byte[] parseSmeProtHErrorBody(ProtHError error) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(
                ProtocolHandshakeErrorMsg.class,
                new ProtHErrorTypeAdapter()
            )
            .create();
        return smeProtMsg(gson
            .toJson(new ProtocolHandshakeErrorMsg(error))
            .getBytes(UTF_8));
    }

    public static byte[] parseSmePinBody(PinMsg pinMsg) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(PinMsg.class, new PinTypeAdapter())
            .create();
        return smePinMsg(gson.toJson(pinMsg).getBytes(UTF_8));
    }

    public static byte[] parseSmePinBody(String pin) {
        PinMsg p = new PinMsg();
        ConnectionPinInputType cpiType = new ConnectionPinInputType(pin);
        p.setPinValue(cpiType);
        return parseSmePinBody(p);
    }

    public static byte[] parseSmePinBody(
        PinStateType pinState,
        @Nullable
        PinInputPermissionType inputPermission
    ) {
        PinMsg p = new PinMsg();
        ConnectionPinStateType cpsType = new ConnectionPinStateType(
            pinState,
            inputPermission
        );
        p.setPinValue(cpsType);
        return parseSmePinBody(p);
    }

    public static byte[] parseCdeBody(CDEMsg cdeMsg) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(CDEMsg.class, new CDETypeAdapter())
            .create();
        return cdeMsg(gson.toJson(cdeMsg).getBytes(UTF_8));
    }

    public static byte[] parseCdeBody(
        String header, String payload,
        @Nullable
        CDEMsg.ExtensionType extType
    ) {
        CDEMsg cdeMsg = new CDEMsg(header, payload, extType);
        return parseCdeBody(cdeMsg);
    }

    public static byte[] parseAmiBody(AccessMethodsRequestMsg amiReqMsg) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(AccessMethodsRequestMsg.class, new AMRTypeAdapter())
            .create();
        return amiMsg(gson.toJson(amiReqMsg).getBytes(UTF_8));
    }

    public static byte[] parseAmiReqBody() {
        return parseAmiBody(new AccessMethodsRequestMsg());
    }

    public static byte[] parseAmiBody(AccessMethodsMsg amiMsg) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(AccessMethodsMsg.class, new AMTypeAdapter())
            .create();
        return amiMsg(gson.toJson(amiMsg).getBytes(UTF_8));
    }

    public static byte[] parseAmiBody(
        String id,
        @Nullable
        AccessMethodsMsg.DnsSd_mDns dnsSd_mDns,
        @Nullable
        AccessMethodsMsg.Dns dns
    ) {
        AccessMethodsMsg amiMsg = new AccessMethodsMsg(id, dnsSd_mDns, dns);
        return parseAmiBody(amiMsg);
    }

    public static byte[] parseConnectionCloseBody(CloseMsg closeMsg) {
        if (closeMsg.getPhase() == null) {
            throw new IllegalArgumentException("phase should not be null");
        }

        Gson gson = new GsonBuilder()
            .registerTypeAdapter(CloseMsg.class, new ConnectionCloseTypeAdapter())
            .create();
        return connectionCloseMsg(gson.toJson(closeMsg).getBytes(UTF_8));
    }

    public static byte[] parseConnectionCloseBody(
        ConnectionClosePhaseType phase,
        @Nullable
        Integer maxTime,
        @Nullable
        ConnectionCloseReasonType reason
    ) {
        CloseMsg closeMsg = new CloseMsg(phase, maxTime, reason);
        return parseConnectionCloseBody(closeMsg);
    }

}
