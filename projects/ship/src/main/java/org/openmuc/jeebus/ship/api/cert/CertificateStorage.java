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
 * This interface allows users to implement and use their own certificate storage
 * with jEEBus.SHIP.
 */
public interface CertificateStorage {
    Optional<CertificateInfo> readCertificate() throws CertificateStoreException;

    void saveCertificate(CertificateInfo certificate) throws CertificateStoreException;
}
