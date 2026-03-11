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

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import javax.annotation.Nonnull;
import java.io.IOException;

public class ProtHErrorTypeAdapter extends TypeAdapter<ProtocolHandshakeErrorMsg> {
    @Override
    public void write(JsonWriter out, ProtocolHandshakeErrorMsg value) throws
        IOException {
        // TODO fix
        out.beginObject();

        out.name(OBJECT_NAME);
        writeError(out, value.getError());

        out.endObject(); // end messageProtocolHandshakeError obj
    }

    private static final String OBJECT_NAME = "messageProtocolHandshakeError";

    private void writeError(JsonWriter out, ProtHError error) throws IOException {
        out.beginArray();
        out.beginObject();
        out.name("error").value(error.getErrorAsByte());
        out.endObject();
        out.endArray();
    }

    @Override
    public ProtocolHandshakeErrorMsg read(JsonReader in) throws IOException {
        ProtocolHandshakeErrorMsg phe = new ProtocolHandshakeErrorMsg();
        in.beginObject();

        phe.setError(readError(in));

        in.endObject();

        return phe;
    }

    private ProtHError readError(JsonReader in) throws IOException {
        if (!in.nextName().equals(OBJECT_NAME))
            throw new JsonParseException("unexpected field name, expected "+OBJECT_NAME);
        in.beginArray();

        in.beginObject();
        if (!in.nextName().equals("error"))
            throw new JsonParseException("unexpected field name, only \"error\" is allowed here");
        ProtHError error = ProtHError.getErrorFromByte((byte) in.nextInt());
        in.endObject();

        in.endArray();

        return error;
    }
}
