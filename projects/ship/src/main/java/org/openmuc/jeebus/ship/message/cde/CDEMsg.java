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

import java.util.Arrays;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CDEMsg {

    private String header;

    // can be any type but for now set to string
    private String payload;

    private ExtensionType extension;

    public CDEMsg() {
    }

    public CDEMsg(String header, String payload) {
        this.header = header;
        this.payload = payload;
    }

    /**
     * constructs a connection data exchange message with the given parameters
     *
     * @param header
     *     protocol id, non-null
     * @param payload
     *     payload of the message, non-null
     * @param extType
     *     extension type, nullable
     */
    public CDEMsg(String header, String payload, ExtensionType extType) {
        this.header = header;
        this.payload = payload;
        this.extension = extType;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public ExtensionType getExtension() {
        return extension;
    }

    public void setExtension(ExtensionType extension) {
        this.extension = extension;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof CDEMsg)) {
            return false;
        }

        final CDEMsg other = (CDEMsg) obj;

        if (!Objects.equals(this.header, other.header)) {
            return false;
        }

        if (!Objects.equals(this.payload, other.payload)) {
            return false;
        }

        return !Objects.equals(this.extension, other.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, payload, extension);
    }

    public static class ExtensionType {
        private String extensionId;

        private byte[] binary;

        private String string;

        public ExtensionType() {
        }

        /**
         * optional, can be used to extend content from payload with manufacturer
         * specific data
         *
         * @param extensionId
         *     Identifier for content, may be used by manufacturer to identify the
         *     kind of content of 'binary' and 'string', nullable
         * @param binary
         *     Binary data
         * @param string
         *     Textual data
         */
        public ExtensionType(String extensionId, byte[] binary, String string) {
            this.extensionId = extensionId;
            this.binary = binary;
            this.string = string;
        }

        /**
         * optional, can be used to extend content from payload with manufacturer
         * specific data
         *
         * @param extensionId
         *     Identifier for content, may be used by manufacturer to identify the
         *     kind of content of 'binary' and 'string', nullable
         * @param binary
         *     Binary data
         * @param string
         *     Textual data
         */
        public ExtensionType(String extensionId, String binary, String string) {
            this.extensionId = extensionId;
            this.binary = binary.getBytes(UTF_8);
            this.string = string;
        }

        public String getExtensionId() {
            return extensionId;
        }

        public void setExtensionId(String extensionId) {
            this.extensionId = extensionId;
        }

        public byte[] getBinary() {
            return binary;
        }

        public void setBinary(byte[] binary) {
            this.binary = binary;
        }

        public String getBinaryAsString() {
            return new String(binary, UTF_8);
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (!(obj instanceof ExtensionType)) {
                return false;
            }

            final ExtensionType other = (ExtensionType) obj;

            if (!Objects.equals(this.extensionId, other.extensionId)) {
                return false;
            }

            if (this.binary == null ?
                other.extensionId != null :
                !Arrays.equals(this.binary, other.binary)) {
                return false;
            }

            return !Objects.equals(this.string, other.string);

        }

        @Override
        public int hashCode() {
            return Objects.hash(extensionId, Arrays.hashCode(binary), string);
        }
    }

}
