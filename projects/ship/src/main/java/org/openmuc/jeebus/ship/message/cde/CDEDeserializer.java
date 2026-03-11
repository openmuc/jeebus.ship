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

import com.google.gson.*;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class CDEDeserializer implements JsonDeserializer<CDEMsg> {

    @Override
    public CDEMsg deserialize(
        JsonElement jsonElement,
        Type type,
        JsonDeserializationContext jsonDeserializationContext
    )
        throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        JsonArray dataArray = obj.getAsJsonArray("data");

        JsonArray headerArray = dataArray
            .get(0)
            .getAsJsonObject()
            .getAsJsonArray("header");
        String protocolId = headerArray
            .get(0)
            .getAsJsonObject()
            .get("protocolId")
            .getAsString();

        JsonElement payloadElem = dataArray.get(1).getAsJsonObject().get("payload");
        String payload = payloadElem.isJsonObject() ?
            payloadElem.getAsJsonObject().toString() :
            payloadElem.getAsString();

        CDEMsg.ExtensionType extensionType = null;
        if (dataArray.size() == 3) {
            // extension
            JsonArray extensionArray = dataArray
                .get(2)
                .getAsJsonObject()
                .getAsJsonArray("extension");
            extensionType = new CDEMsg.ExtensionType();
            String extId = "extensionId";
            String bin = "binary";
            String str = "string";
            for (JsonElement extensionElem : extensionArray) {
                extensionElem.getAsJsonObject();
                JsonObject extensionObj = extensionElem.getAsJsonObject();
                if (extensionObj.has(extId)) {
                    extensionType.setExtensionId(extensionObj
                        .get(extId)
                        .getAsString());
                }
                else if (extensionObj.has(bin)) {
                    extensionType.setBinary(extensionObj
                        .get(bin)
                        .getAsString()
                        .getBytes(StandardCharsets.UTF_8));
                }
                else if (extensionObj.has(str)) {
                    extensionType.setString(extensionObj.get(str).getAsString());
                }
            }
        }

        return new CDEMsg(protocolId, payload, extensionType);
    }
}
