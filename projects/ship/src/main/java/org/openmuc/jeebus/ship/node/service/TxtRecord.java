/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node.service;

import org.openmuc.jeebus.ship.message.MessageUtility;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TxtRecord {

    private final Map<String, String> props = new LinkedHashMap<>();

    /**
     * @param id
     *     globally unique ID of the SHIP node
     * @param path
     *     String with wss path
     * @param ski
     *     40 digit hexadecimal string representing the 160 bit secp256r1 SKI value,
     *     for SHIP node identification
     * @param register
     *     o indicate whether auto accept is active in the SHIP node
     */
    public TxtRecord(String id, String path, String ski, boolean register) {
        // version number
        props.put("txtvers", "1");
        if (id.getBytes(UTF_8).length > 63) {
            throw new IllegalArgumentException(
                "id should not be longer than 63 bytes");
        }

        // globally unique id of SHIP node, first part should be abbreviation of manufacturer name
        props.put("id", id);

        if (path.getBytes(UTF_8).length == 0) {
            throw new IllegalArgumentException("path should be at least 1 byte long");
        }
        else if (!(path.startsWith("/"))) {
            throw new IllegalArgumentException("path should start with '/'");
        }
        else if (path.getBytes(UTF_8).length > 32) {
            throw new IllegalArgumentException(
                "path should not be longer than 32 bytes");
        }
        else if (path.getBytes(UTF_8).length == 1 && !path.equals("/")) {
            // if path is 1 byte long, only '/' is allowed
            throw new IllegalArgumentException("path is invalid");
        }
        // wss path
        props.put("path", path);

        if (!MessageUtility.isHexDigits(ski)) {
            throw new IllegalArgumentException(
                "SKI does not represent hexadecimal digits");
        }
        else if (ski.getBytes(UTF_8).length != 40) {
            throw new IllegalArgumentException(
                "SKI hexadecimal digit value should be 40 bytes long");
        }
        // ski value derived from public key of SHIP node
        props.put("ski", ski);
        // used to indicate whether auto accept is active in SHIP node
        props.put("register", String.valueOf(register));
    }

    /**
     * also adds optional parameters
     *
     * @param id
     *     globally unique ID of the SHIP node
     * @param path
     *     String with wss path
     * @param ski
     *     40 digit hexadecimal string representing the 160 bit secp256r1 SKI value,
     *     for SHIP node identification
     * @param register
     *     o indicate whether auto accept is active in the SHIP node
     * @param brand
     *     brand of the device
     * @param type
     *     device type
     * @param model
     *     device's model
     */
    public TxtRecord(
        String id,
        String path,
        String ski,
        boolean register,
        String brand,
        String type,
        String model
    ) {
        this(id, path, ski, register);
        props.put("brand", brand);
        props.put("type", type);
        props.put("model", model);
    }

    public Map<String, String> getTxtRecordProps() {
        return props;
    }

    public int getTxtvers() {
        return Integer.parseInt(props.get("txtvers"));
    }

    public String getId() {
        return props.get("id");
    }

    public void setId(String id) {
        props.put("id", id);
    }

    public String getPath() {
        return props.get("path");
    }

    public void setPath(String path) {
        props.put("path", path);
    }

    public String getSki() {
        return props.get("ski");
    }

    public void setSki(String ski) {
        props.put("ski", ski);
    }

    public boolean getRegister() {
        return Boolean.parseBoolean(props.get("register"));
    }

    public void setRegister(boolean register) {
        props.put("register", String.valueOf(register));
    }

    public String getBrand() {
        return props.get("brand");
    }

    public void setBrand(String brand) {
        props.put("brand", brand);
    }

    public String getType() {
        return props.get("type");
    }

    public void setType(String type) {
        props.put("type", type);
    }

    public String getModel() {
        return props.get("model");
    }

    public void setModel(String model) {
        props.put("model", model);
    }
}
