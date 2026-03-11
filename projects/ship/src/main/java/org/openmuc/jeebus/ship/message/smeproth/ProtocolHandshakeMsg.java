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

import java.util.List;
import java.util.Objects;

public class ProtocolHandshakeMsg {
    private ProtocolHandshakeTypeType handshakeType;

    private int major;
    private int minor;

    private List<String> formats;

    /**
     * no-argument constructor for serialization/deserialization
     */
    public ProtocolHandshakeMsg() {
    }

    public ProtocolHandshakeMsg(
        ProtocolHandshakeTypeType type,
        int major,
        int minor,
        List<String> formats
    ) {
        this.handshakeType = type;
        this.major = major;
        this.minor = minor;
        this.formats = formats;
    }

    public ProtocolHandshakeTypeType getHandshakeType() {
        return handshakeType;
    }

    public void setHandshakeType(ProtocolHandshakeTypeType handshakeType) {
        this.handshakeType = handshakeType;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public List<String> getFormats() {
        return formats;
    }

    public void setFormats(List<String> formats) {
        this.formats = formats;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof ProtocolHandshakeMsg)) {
            return false;
        }

        final ProtocolHandshakeMsg other = (ProtocolHandshakeMsg) obj;

        if (this.handshakeType == null ?
            other.handshakeType != null :
            this.handshakeType != other.handshakeType) {
            return false;
        }

        if (this.minor != other.minor) {
            return false;
        }

        if (this.major != other.major) {
            return false;
        }

        return this.formats.equals(other.formats);

    }

    @Override
    public int hashCode() {
        return Objects.hash(handshakeType, major, minor, formats);
    }
}
