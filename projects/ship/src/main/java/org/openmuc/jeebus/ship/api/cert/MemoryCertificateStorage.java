/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.api.cert;

import java.util.Optional;

/**
 * Certificate storage that stores the certificate only in memory.
 */
public class MemoryCertificateStorage implements CertificateStorage {
    private CertificateInfo certificate;

    @Override
    public Optional<CertificateInfo> readCertificate() {
        return Optional.ofNullable(this.certificate);
    }

    @Override
    public void saveCertificate(CertificateInfo certificate) {
        this.certificate = certificate;
    }

    @Override
    public String toString() {
        return "MemoryCertificateStorage{}";
    }
}
