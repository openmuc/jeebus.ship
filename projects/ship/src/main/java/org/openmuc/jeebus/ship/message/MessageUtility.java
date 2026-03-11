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

import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.bouncycastle.util.encoders.Hex;
import org.openmuc.jeebus.ship.message.ami.AMRTypeAdapter;
import org.openmuc.jeebus.ship.message.ami.AMTypeAdapter;
import org.openmuc.jeebus.ship.message.ami.AccessMethodsMsg;
import org.openmuc.jeebus.ship.message.ami.AccessMethodsRequestMsg;
import org.openmuc.jeebus.ship.message.cde.CDEDeserializer;
import org.openmuc.jeebus.ship.message.cde.CDEMsg;
import org.openmuc.jeebus.ship.message.connectionclose.CloseMsg;
import org.openmuc.jeebus.ship.message.connectionclose.ConnectionCloseTypeAdapter;
import org.openmuc.jeebus.ship.message.smehello.HelloMsg;
import org.openmuc.jeebus.ship.message.smehello.HelloTypeAdapter;
import org.openmuc.jeebus.ship.message.smepin.PinMsg;
import org.openmuc.jeebus.ship.message.smepin.PinTypeAdapter;
import org.openmuc.jeebus.ship.message.smeproth.ProtHTypeAdapter;
import org.openmuc.jeebus.ship.message.smeproth.ProtocolHandshakeMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * holds static utility methods related to message validation, de-/serialization and
 * parsing preprocess methods separate MessageType (first byte in every message) from
 * MessageValue
 */
public class MessageUtility {

    private static final Logger log = LoggerFactory.getLogger(MessageUtility.class);

    private MessageUtility() {
    }

    public static BinaryWebSocketFrame wrapInBinaryFrame(byte[] msg) {
        if (msg == null) {
            throw new IllegalArgumentException("message array should not be null");
        }

        ByteBuf buf = Unpooled.wrappedBuffer(msg);
        return new BinaryWebSocketFrame(buf);
    }

    public static HelloMsg deserializeHello(byte[] json) {
        String jsonString = new String(json, UTF_8);

        return new GsonBuilder()
            .registerTypeAdapter(HelloMsg.class, new HelloTypeAdapter())
            .create()
            .fromJson(jsonString, HelloMsg.class);
    }

    public static HelloMsg preprocessHelloMsg(byte[] msg) {
        if (msg[0] != 1) {
            throw new IllegalArgumentException(
                "received a SME_HELLO message with MessageType other than 1");
        }

        byte[] json = new byte[msg.length - 1];
        System.arraycopy(msg, 1, json, 0, msg.length - 1);
        return deserializeHello(json);
    }

    public static ProtocolHandshakeMsg deserializeProtH(byte[] json) {
        String jsonString = new String(json, UTF_8);

        return new GsonBuilder()
            .registerTypeAdapter(ProtocolHandshakeMsg.class, new ProtHTypeAdapter())
            .create()
            .fromJson(jsonString, ProtocolHandshakeMsg.class);
    }

    /**
     * If there are messages in the message queue, removes the message from the
     * message queue and splits header from body and parses it into a
     * ProtocolHandshake object.
     *
     * @param msg
     *     array of received message (message queue)
     * @return the parsed ProtocolHandshake object
     * @throws IllegalStateException
     *     if something went wrong
     */
    public static ProtocolHandshakeMsg preprocessProtHMsg(byte[] msg) {
        if (msg == null) {
            throw new IllegalArgumentException("received message was null");
        }
        if (msg[0] != 1) {
            throw new IllegalArgumentException(
                "received a SME_PROT_H message with MessageType other than 1");
        }
        byte[] json = new byte[msg.length - 1];
        System.arraycopy(msg, 1, json, 0, msg.length - 1);
        return deserializeProtH(json);
    }

    public static PinMsg deserializePin(byte[] json) {
        String jsonString = new String(json, UTF_8);

        return new GsonBuilder()
            .registerTypeAdapter(PinMsg.class, new PinTypeAdapter())
            .create()
            .fromJson(jsonString, PinMsg.class);
    }

    public static PinMsg preprocessPinMsg(byte[] msg) {
        if (msg == null) {
            throw new IllegalArgumentException("received message was null");
        }
        if (msg[0] != 1) {
            throw new IllegalArgumentException(
                "received a PIN message with MessageType other than 1");
        }

        byte[] json = new byte[msg.length - 1];
        System.arraycopy(msg, 1, json, 0, msg.length - 1);
        return deserializePin(json);
    }

    public static CDEMsg deserializeCde(byte[] json) {
        String jsonString = new String(json, UTF_8);

        return new GsonBuilder()
            .registerTypeAdapter(CDEMsg.class, new CDEDeserializer())
            .create()
            .fromJson(jsonString, CDEMsg.class);
    }

    public static CDEMsg preprocessCDEMsg(byte[] msg) {
        if (msg[0] != 2) {
            throw new IllegalArgumentException(
                "received a CDE message with MessageType other than 2");
        }

        byte[] json = new byte[msg.length - 1];
        System.arraycopy(msg, 1, json, 0, msg.length - 1);
        return deserializeCde(json);
    }

    public static AccessMethodsRequestMsg deserializeAmr(byte[] json) {
        String jsonString = new String(json, UTF_8);

        return new GsonBuilder()
            .registerTypeAdapter(AccessMethodsRequestMsg.class, new AMRTypeAdapter())
            .create()
            .fromJson(jsonString, AccessMethodsRequestMsg.class);
    }

    public static AccessMethodsRequestMsg preprocessAmrMsg(byte[] msg) {
        if (msg == null) {
            throw new IllegalArgumentException("received message was null");
        }
        if (msg[0] != 1) {
            throw new IllegalArgumentException(
                "received a AMR message with MessageType other than 1");
        }

        byte[] json = new byte[msg.length - 1];
        System.arraycopy(msg, 1, json, 0, msg.length - 1);
        return deserializeAmr(json);
    }

    public static AccessMethodsMsg deserializeAm(byte[] json) {
        String jsonString = new String(json, UTF_8);

        return new GsonBuilder()
            .registerTypeAdapter(AccessMethodsMsg.class, new AMTypeAdapter())
            .create()
            .fromJson(jsonString, AccessMethodsMsg.class);
    }

    public static AccessMethodsMsg preprocessAmMsg(byte[] msg) {
        if (msg == null) {
            throw new IllegalArgumentException("received message was null");
        }
        if (msg[0] != 1) {
            throw new IllegalArgumentException(
                "received a AM message with MessageType other than 1");
        }

        byte[] json = new byte[msg.length - 1];
        System.arraycopy(msg, 1, json, 0, msg.length - 1);
        return deserializeAm(json);
    }

    public static CloseMsg deserializeConnCloseMsg(byte[] json) {
        String jsonString = new String(json, UTF_8);

        return new GsonBuilder()
            .registerTypeAdapter(CloseMsg.class, new ConnectionCloseTypeAdapter())
            .create()
            .fromJson(jsonString, CloseMsg.class);
    }

    public static CloseMsg preprocessConnCloseMsg(byte[] msg) {
        if (msg[0] != 3) {
            throw new IllegalArgumentException(
                "received a ConnectionClose message with MessageType other than 3");
        }

        byte[] json = new byte[msg.length - 1];
        System.arraycopy(msg, 1, json, 0, msg.length - 1);
        return deserializeConnCloseMsg(json);
    }

    public static boolean isHexDigits(String s) {
        return s.matches("[0-9a-fA-F]+");
    }

    /**
     * encode byte array to hexadecimal string, not needed if restrict pin
     *
     * @param bytes
     *     byte array containing the hexadecimal values
     * @return the encoded hex
     */
    public static String encodeHex(byte[] bytes) {
        return Hex.toHexString(bytes);
    }

    /**
     * decode hexadecimal string to byte array
     *
     * @param hexString
     *     the hexadecimal string to decode
     * @return the decoded hexadecimal string
     */
    public static byte[] decodeHex(String hexString) {
        return Hex.decode(hexString);
    }

    /**
     * Inserts a whitespace after every 4 characters for better graphical
     * presentation. Also cuts off '0x' at the beginning of the String if it exists.
     *
     * @param hexString
     *     the hexadecimal string to convert
     * @return the pretty print version of the string
     */
    public static String hexToPrettyFormat(String hexString) {
        if (hexString.startsWith("0x")) {
            return hexString.substring(2).replaceAll(".{4}", "$0 ");
        }
        return hexString.replaceAll(".{4}", "$0 ");
    }

    /**
     * constructs and formats an error message
     *
     * @param errorType
     *     the error type
     * @return the error message as a byte array
     */
    public static byte[] createErrorMsg(int errorType) {
        String errorJson = String.format("{\"error\":[%s]}", errorType);
        return errorJson.getBytes(UTF_8);
    }

    /**
     * checks if PIN is valid, removes all spaces before checking
     *
     * @param pin
     *     the PIN to be checked
     * @return <code>true</code> if PIN is valid, <code>false</code> otherwise
     */
    public static boolean isValidPin(String pin) {
        // hex string with a length between 8 and 16
        return pin.replaceAll(" ", "").matches("[0-9a-fA-F]{8,16}");
    }

    /**
     * parse a message for logging
     *
     * @param msg
     *     the received message
     * @return the parsed message
     */
    public static String parseShipMsgToString(byte[] msg) {
        // first byte should always be binary data, MessageType, which requires different encoding
        String msgType = Byte.toString(msg[0]);

        // encode the MessageValue which is text data
        String msgValString = new String(msg, 1, msg.length-1, UTF_8);

        return String.format(
            "MessageType: %s\nMessageValue:\n%s",
            msgType,
            msgValString
        );
    }

    /**
     * parse a cmi message for logging. Requires special handling because it only
     * contains binary data
     *
     * @param msg
     *     the error message or CMI_STATE msg
     * @return the parsed message
     */
    public static String parseCmiMsgToString(byte[] msg) {
        return String.format("MessageType: %s\nMessageValue: %s", msg[0], msg[1]);
    }

    // the following helper methods for de-/serialization

    // wrap an element in an object
    public static void writeStringToObject(
        JsonWriter out,
        String property,
        String value
    ) throws IOException {
        out.beginObject();
        out.name(property).value(value);
        out.endObject();
    }

    public static void writeIntToObject(
        JsonWriter out,
        String property,
        Integer value
    ) throws IOException {
        out.beginObject();
        out.name(property).value(value);
        out.endObject();
    }

    public static void writeBooleanToObject(
        JsonWriter out,
        String property,
        Boolean value
    ) throws IOException {
        out.beginObject();
        out.name(property).value(value);
        out.endObject();
    }

    // read a wrapped element
    public static String readWrappedString(JsonReader in) throws IOException {
        String val = in.nextString();
        in.endObject();
        return val;
    }

    public static int readWrappedInt(JsonReader in) throws IOException {
        int val = in.nextInt();
        in.endObject();
        return val;
    }

    public static boolean readWrappedBoolean(JsonReader in) throws IOException {
        boolean val = in.nextBoolean();
        in.endObject();
        return val;
    }

    public static void beginObjectIfHasNext(JsonReader in) throws IOException {
        if (in.peek().name().equals("BEGIN_OBJECT")) {
            in.beginObject();
        }
    }

    public static void checkFieldName(JsonReader in, String desiredName) throws
        IOException {
        String fieldName = in.nextName();
        if (!fieldName.equals(desiredName)) {
            throw new IllegalArgumentException("field "
                + fieldName
                + " not recognized");
        }
    }

}
