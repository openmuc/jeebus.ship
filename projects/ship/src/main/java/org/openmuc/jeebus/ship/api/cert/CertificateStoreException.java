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

/**
 * Thrown when a CertificateStorage failed to load or store a certificate.
 */
public class CertificateStoreException extends Exception {

    public CertificateStoreException(String message, Exception innerException) {
        super(message, innerException);
    }
}
