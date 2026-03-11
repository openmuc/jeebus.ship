/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message.cde;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.openmuc.jeebus.ship.message.MessageUtility.*;

public class CDETypeAdapter extends TypeAdapter<CDEMsg> {
    @Override
    public void write(JsonWriter out, CDEMsg value) throws IOException {
        out.beginObject();

        out.name("data");
        out.beginArray();

        writeHeader(out, value.getHeader());

        out.beginObject();
        out.name("payload").jsonValue(value.getPayload());
        out.endObject();

        if (value.getExtension() != null) {
            writeExtension(out, value.getExtension());
        }

        out.endArray(); // end data array
        out.endObject();
    }

    private void writeHeader(JsonWriter out, String protocolId) throws IOException {
        out.beginObject();
        out.name("header");
        out.beginArray();
        writeStringToObject(out, "protocolId", protocolId);
        out.endArray();
        out.endObject();
    }

    private void writeExtension(JsonWriter out, CDEMsg.ExtensionType extType) throws
        IOException {
        out.beginObject();
        out.name("extension");
        out.beginArray();
        if (extType.getExtensionId() != null) {
            writeStringToObject(out, "extensionId", extType.getExtensionId());
        }
        if (extType.getBinary() != null) {
            writeStringToObject(out, "binary", extType.getBinaryAsString());
        }
        if (extType.getString() != null) {
            writeStringToObject(out, "string", extType.getString());
        }
        out.endArray();
        out.endObject();
    }

    @Override
    public CDEMsg read(JsonReader in) throws IOException {
        // TODO: either keep using TypeAdapter for serialization or write a JsonSerializer
        throw new IOException(
            "TypeAdapter should not be used for CDE message deserialization");
    }

    private String readHeader(JsonReader in) throws IOException {
        in.beginArray();
        in.beginObject();
        in.nextName(); // consume header
        String header = readWrappedString(in);
        in.endArray();
        in.endObject();
        return header;
    }

    private String readPayload(JsonReader in) throws IOException {
        String payload = "";
        in.endObject();
        return payload;
    }

    private CDEMsg.ExtensionType readExtension(JsonReader in) throws IOException {
        CDEMsg.ExtensionType extType = new CDEMsg.ExtensionType();
        in.beginArray();
        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "extensionId":
                    extType.setExtensionId(readWrappedString(in));
                    beginObjectIfHasNext(in);
                    break;
                case "binary":
                    extType.setBinary(readWrappedString(in).getBytes(UTF_8));
                    beginObjectIfHasNext(in);
                    break;
                case "string":
                    extType.setString(readWrappedString(in));
                    beginObjectIfHasNext(in);
                    break;
                default:
                    throw new IllegalArgumentException("field not recognized");
            }
        }
        in.endArray();
        in.endObject();
        return extType;
    }
}
