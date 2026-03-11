/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message.smeproth;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.openmuc.jeebus.ship.message.MessageUtility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.openmuc.jeebus.ship.message.smeproth.ProtocolHandshakeTypeType.getByValue;

public class ProtHTypeAdapter extends TypeAdapter<ProtocolHandshakeMsg> {
    @Override
    public void write(JsonWriter out, ProtocolHandshakeMsg value) throws
        IOException {
        out.beginObject();
        out.name("messageProtocolHandshake");
        out.beginArray();

        writeHandshakeType(out, value.getHandshakeType().toString());

        writeVersion(out, value.getMajor(), value.getMinor());

        writeFormats(out, value.getFormats());

        out.endArray(); // end messageProtocolHandshake array
        out.endObject(); // end messageProtocolHandshake obj
    }

    private void writeHandshakeType(JsonWriter out, String handshakeType) throws
        IOException {
        out.beginObject();
        out.name("handshakeType").value(handshakeType);
        out.endObject();
    }

    private void writeVersion(JsonWriter out, int major, int minor) throws
        IOException {
        out.beginObject();
        out.name("version");
        out.beginArray();

        out.beginObject();
        out.name("major").value(major);
        out.endObject();

        out.beginObject();
        out.name("minor").value(minor);
        out.endObject();

        out.endArray();
        out.endObject(); // end version
    }

    private void writeFormats(JsonWriter out, List<String> formats) throws
        IOException {
        out.beginObject();
        out.name("formats");
        out.beginArray();

        out.beginObject();
        out.name("format");
        out.beginArray();
        for (String format : formats) {
            out.value(format);
        }
        out.endArray();
        out.endObject();

        out.endArray();
        out.endObject(); // end formats
    }

    @Override
    public ProtocolHandshakeMsg read(JsonReader in) throws IOException {
        ProtocolHandshakeMsg ph = new ProtocolHandshakeMsg();

        in.beginObject();
        MessageUtility.checkFieldName(in, "messageProtocolHandshake");
        in.beginArray();
        in.beginObject();

        while (in.hasNext()) {
            switch (in.nextName()) {
                case "handshakeType":
                    ph.setHandshakeType(readHandshakeType(in));
                    in.endObject();
                    in.beginObject();
                    break;
                case "version":
                    readVersion(in, ph);
                    in.endObject();
                    in.beginObject();
                    break;
                case "formats":
                    ph.setFormats(readFormats(in));
                    in.endObject();
                    break;
                default:
                    throw new IllegalArgumentException("field not recognized");
            }
        }

        in.endArray(); // end messageProtocolHandshake array
        in.endObject(); // end messageProtocolHandshake obj

        return ph;
    }

    private ProtocolHandshakeTypeType readHandshakeType(JsonReader in) throws
        IOException {
        return getByValue(in.nextString());
    }

    private void readVersion(JsonReader in, ProtocolHandshakeMsg ph) throws
        IOException {
        in.beginArray();

        while (in.hasNext()) {
            in.beginObject();
            if (in.nextName().equals("major")) {
                ph.setMajor(in.nextInt());
            }
            else {
                ph.setMinor(in.nextInt());
            }
            in.endObject();
        }

        in.endArray();
    }

    private List<String> readFormats(JsonReader in) throws IOException {
        List<String> formats = new ArrayList<>();

        in.beginArray();

        in.beginObject();
        in.nextName(); // consume format
        in.beginArray();
        while (in.hasNext()) {
            formats.add(in.nextString());
        }
        in.endArray();
        in.endObject();

        in.endArray();
        return formats;
    }
}
