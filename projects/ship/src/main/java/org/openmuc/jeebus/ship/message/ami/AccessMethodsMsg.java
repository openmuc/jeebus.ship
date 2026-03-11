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

import java.util.Objects;

public class AccessMethodsMsg {

    private String id;

    private DnsSd_mDns dnsSd_mDns;

    private Dns dns;

    public AccessMethodsMsg() {
    }

    public AccessMethodsMsg(String id, DnsSd_mDns dnsSd_mDns, Dns dns) {
        this.id = id;
        this.dnsSd_mDns = dnsSd_mDns;
        this.dns = dns;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DnsSd_mDns getDnsSd_mDns() {
        return dnsSd_mDns;
    }

    public void setDnsSd_mDns(DnsSd_mDns dnsSd_mDns) {
        this.dnsSd_mDns = dnsSd_mDns;
    }

    public Dns getDns() {
        return dns;
    }

    public void setDns(Dns dns) {
        this.dns = dns;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof AccessMethodsMsg)) {
            return false;
        }

        final AccessMethodsMsg other = (AccessMethodsMsg) obj;

        if (!Objects.equals(this.id, other.id)) {
            return false;
        }

        if (!Objects.equals(this.dnsSd_mDns, other.dnsSd_mDns)) {
            return false;
        }

        return Objects.equals(this.dns, other.dns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dnsSd_mDns, dns);
    }

    public static class DnsSd_mDns {
        public DnsSd_mDns() {
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            return obj instanceof DnsSd_mDns;
        }
    }

    public static class Dns {
        private String uri;

        public Dns() {
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (!(obj instanceof Dns)) {
                return false;
            }

            final Dns other = (Dns) obj;

            return Objects.equals(this.uri, other.uri);
        }
    }

}
