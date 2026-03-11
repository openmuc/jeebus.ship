/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message.connectionclose;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.openmuc.jeebus.ship.message.MessageUtility;

import java.io.IOException;

public class ConnectionCloseTypeAdapter extends TypeAdapter<CloseMsg> {

    @Override
    public void write(JsonWriter out, CloseMsg value) throws IOException {
        out.beginObject();

        out.name("connectionClose");
        out.beginArray();

        MessageUtility.writeStringToObject(
            out,
            "phase",
            value.getPhase().toString()
        );

        Integer maxTime = value.getMaxTime();
        if (maxTime != null) {
            if (maxTime < 0) {
                throw new IllegalArgumentException("max time should be positive");
            }
            MessageUtility.writeIntToObject(out, "maxTime", maxTime);
        }

        ConnectionCloseReasonType reason = value.getReason();
        if (reason != null) {
            MessageUtility.writeStringToObject(out, "reason", reason.toString());
        }

        out.endArray();
        out.endObject();
    }

    @Override
    public CloseMsg read(JsonReader in) throws IOException {
        CloseMsg closeMsg = new CloseMsg();

        in.beginObject();

        MessageUtility.checkFieldName(in, "connectionClose");
        in.beginArray();

        // no need to process phase in while loop, as phase is always required
        in.beginObject();
        MessageUtility.checkFieldName(in, "phase");
        closeMsg.setPhase(ConnectionClosePhaseType.getByValue(MessageUtility.readWrappedString(
            in)));

        while (in.hasNext()) {
            in.beginObject();
            String nextField = in.nextName();
            switch (nextField) {
                case "maxTime":
                    closeMsg.setMaxTime(MessageUtility.readWrappedInt(in));
                    break;
                case "reason":
                    closeMsg.setReason(ConnectionCloseReasonType.getByValue(
                        MessageUtility.readWrappedString(in)));
                    break;
                default:
                    throw new IllegalArgumentException("field "
                        + nextField
                        + " not recognized");
            }
        }

        in.endArray();
        in.endObject();

        return closeMsg;
    }
}
