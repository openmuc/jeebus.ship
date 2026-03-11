/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message.ami;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.openmuc.jeebus.ship.message.MessageUtility;

import java.io.IOException;

public class AMRTypeAdapter extends TypeAdapter<AccessMethodsRequestMsg> {
    @Override
    public void write(JsonWriter out, AccessMethodsRequestMsg value) throws
        IOException {
        if (value == null) {
            throw new IllegalArgumentException(
                "access methods request should not be null");
        }

        out.beginObject();

        out.name("accessMethodsRequest");
        out.beginArray();
        out.endArray();

        out.endObject();
    }

    @Override
    public AccessMethodsRequestMsg read(JsonReader in) throws IOException {
        in.beginObject();

        MessageUtility.checkFieldName(in, "accessMethodsRequest");

        in.beginArray();
        in.endArray();

        in.endObject();
        return new AccessMethodsRequestMsg();
    }
}
