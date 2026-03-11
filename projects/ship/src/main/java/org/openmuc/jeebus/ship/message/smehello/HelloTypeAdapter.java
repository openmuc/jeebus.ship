/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message.smehello;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.openmuc.jeebus.ship.message.MessageUtility;

import java.io.IOException;

public class HelloTypeAdapter extends TypeAdapter<HelloMsg> {
    @Override
    public void write(JsonWriter out, HelloMsg value) throws IOException {
        out.beginObject();
        out.name("connectionHello");
        out.beginArray();

        ConnectionHelloType parameters = value.getConnectionHello();
        MessageUtility.writeStringToObject(
            out,
            "phase",
            parameters.getPhase().toString()
        );

        if (parameters.getWaiting() != null) {
            MessageUtility.writeIntToObject(out, "waiting", parameters.getWaiting());
        }
        if (parameters.getProlongationRequest() != null) {
            MessageUtility.writeBooleanToObject(
                out,
                "prolongationRequest",
                parameters.getProlongationRequest()
            );
        }

        out.endArray();
        out.endObject();
    }

    @Override
    public HelloMsg read(JsonReader in) throws IOException {
        HelloMsg helloMsg = new HelloMsg();

        ConnectionHelloType chType = new ConnectionHelloType();
        in.beginObject();
        MessageUtility.checkFieldName(in, "connectionHello");
        in.beginArray();
        in.beginObject();

        while (in.hasNext()) {
            switch (in.nextName()) {
                case "phase":
                    String phase = MessageUtility.readWrappedString(in);
                    chType.setPhase(ConnectionHelloPhaseType.getByValue(phase));
                    MessageUtility.beginObjectIfHasNext(in);
                    break;
                case "waiting":
                    chType.setWaiting(MessageUtility.readWrappedInt(in));
                    MessageUtility.beginObjectIfHasNext(in);
                    break;
                case "prolongationRequest":
                    chType.setProlongationRequest(MessageUtility.readWrappedBoolean(
                        in));
                    break;
                default:
                    throw new IllegalArgumentException("field not recognized");
            }
        }

        in.endArray(); // end connectionHello array
        in.endObject();

        helloMsg.setConnectionHello(chType);
        return helloMsg;
    }
}
