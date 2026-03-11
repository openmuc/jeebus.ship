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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AMTypeAdapter extends TypeAdapter<AccessMethodsMsg> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void write(JsonWriter out, AccessMethodsMsg value) throws IOException {
        out.beginObject();

        out.name("accessMethods");
        out.beginArray();

        MessageUtility.writeStringToObject(
            out,
            "id",
            value.getId() == null ? "" : value.getId()
        );

        if (value.getDnsSd_mDns() != null) {
            writeDnsSd_mDns(out, value.getDnsSd_mDns());
        }

        if (value.getDns() != null) {
            writeDns(out, value.getDns());
        }

        out.endArray();
        out.endObject();

    }

    private void writeDnsSd_mDns(
        JsonWriter out,
        AccessMethodsMsg.DnsSd_mDns dnsSd_mDns
    ) throws IOException {
        out.beginObject();
        out.name("dnsSd_mDns");
        out.beginArray();
        out.endArray();
        out.endObject();
    }

    private void writeDns(JsonWriter out, AccessMethodsMsg.Dns dns) throws
        IOException {
        String uri = dns.getUri();
        if (uri == null) {
            throw new IllegalArgumentException(
                "access method response contains \"dns\" field but no value for \"uri\" field");
        }

        if (uri.equals("")) {
            log.warn("access method response contains empty string for \"uri\" field");
        }

        out.beginObject();
        out.name("dns");
        out.beginArray();
        MessageUtility.writeStringToObject(out, "uri", uri);
        out.endArray();
        out.endObject();
    }

    @Override
    public AccessMethodsMsg read(JsonReader in) throws IOException {
        AccessMethodsMsg amMsg = new AccessMethodsMsg();
        in.beginObject();

        MessageUtility.checkFieldName(in, "accessMethods");

        in.beginArray();

        in.beginObject();
        MessageUtility.checkFieldName(in, "id");
        String id = MessageUtility.readWrappedString(in);
        amMsg.setId(id);

        while (in.hasNext()) {
            MessageUtility.beginObjectIfHasNext(in);
            switch (in.nextName()) {
                case "dnsSd_mDns":
                    in.beginArray();
                    in.endArray();
                    in.endObject();
                    amMsg.setDnsSd_mDns(new AccessMethodsMsg.DnsSd_mDns());
                    break;
                case "dns":
                    in.beginArray();
                    in.beginObject();
                    MessageUtility.checkFieldName(in, "uri");
                    String uri = MessageUtility.readWrappedString(in);
                    AccessMethodsMsg.Dns dns = new AccessMethodsMsg.Dns();
                    dns.setUri(uri);
                    amMsg.setDns(dns);
                    in.endArray();
                    in.endObject();
                    break;
                default:
                    throw new IllegalArgumentException("field not recognized");
            }
        }

        in.endArray();

        in.endObject();

        return amMsg;
    }
}
