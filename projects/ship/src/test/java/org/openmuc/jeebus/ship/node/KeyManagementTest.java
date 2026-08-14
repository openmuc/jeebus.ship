/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.node;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.openmuc.jeebus.ship.api.cert.CertificateInfo;
import org.openmuc.jeebus.ship.api.cert.CertificateStorage;
import org.openmuc.jeebus.ship.api.cert.CertificateStoreException;
import org.openmuc.jeebus.ship.api.cert.KeyStoreCertificateStorage;
import org.openmuc.jeebus.ship.api.cert.MemoryCertificateStorage;

import java.io.File;
import java.nio.file.Path;
import java.security.cert.CertificateEncodingException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@Execution(SAME_THREAD)
public class KeyManagementTest {
    private final String keystoreAlias = "testKeys";
    private final char[] keystorePassphrase
        = "qyeditwsnj3k3l2sw7lt9sjlprahk1j0".toCharArray();
    private final char[] keystoreKeyPassphrase
        = "othj88udf6u7d2jjqmm37ysu5gx6vhei".toCharArray();
    private final String dn = "CN=Test";
    private final String shipId = "Test";
    private final int days = 1;
    private final String testSki = "1234AAAAFFFF1111CCCC3333EEEEDDDD99992222";
    private final String testSki2 = "1234AAAAFFFF1111CCCC3333EEEEDDDD99992223";

    @TempDir
    Path tempDir;
    private KeyManagement testManagement;

    @BeforeEach
    public void setUp() throws CertificateStoreException {
        testManagement = new KeyManagement(
            new MemoryCertificateStorage(),
            dn,
            shipId,
            days
        );
    }

    @AfterEach
    public void tearDown() {
        testManagement.clearTrustedSkis();
    }

    @Test
    public void test_file_keystore() throws CertificateStoreException, CertificateEncodingException {
        CertificateStorage storage = this.createKeyStoreCertificateStorage();
        KeyManagement km = new KeyManagement(storage, dn, shipId, days);

        byte[] encodedPrivateKey = km.getCert().privateKey.getEncoded();
        byte[] encodedX509Cert = km.getCert().certificate.getEncoded();

        CertificateStorage storage2 = this.createKeyStoreCertificateStorage();
        var storedCertificate = storage2.readCertificate();

        assertThat(storedCertificate.isPresent(), is(true));
        assertThat(storedCertificate.get().certificate.getEncoded(), is(encodedX509Cert));
        assertThat(storedCertificate.get().privateKey.getEncoded(), is(encodedPrivateKey));
    }

    private CertificateStorage createKeyStoreCertificateStorage() {
        String pathToKeyStore = tempDir.toString() + File.separator + "keystore.jks";
        return new KeyStoreCertificateStorage(
            pathToKeyStore,
            keystoreAlias,
            keystorePassphrase.clone(),
            keystoreKeyPassphrase.clone()
        );
    }

    @Test
    public void test_trusted_SKI() {
        testManagement.addTrustedSki(testSki, 32);
        assertThat(testManagement.getTrustedSkis().size(), is(1));
        assertThat(
            testManagement.getTrustedSkis().get(testSki).getTrustLevel(),
            is(32)
        );

        // adding the same ski twice should not override the previous entry
        testManagement.addTrustedSki(testSki, 64);
        assertThat(testManagement.getTrustedSkis().size(), is(1));
        assertThat(
            testManagement.getTrustedSkis().get(testSki).getTrustLevel(),
            is(32)
        );

        testManagement.addTrustedSki(testSki2, 64);
        assertThat(testManagement.getTrustedSkis().size(), is(2));
        assertThat(
            testManagement.getTrustedSkis().get(testSki2).getTrustLevel(),
            is(64)
        );

        testManagement.removeTrustedSki(testSki);
        assertThat(testManagement.getTrustedSkis().size(), is(1));
        assertThat(testManagement.getTrustedSkis(), hasKey(testSki2));
        assertThat(
            testManagement.getTrustedSkis().get(testSki2).getTrustLevel(),
            is(64)
        );
        assertThat(testManagement.getTrustedSkis(), not(hasKey(testSki)));

        testManagement.addTrustedSki(testSki, 32);
        testManagement.clearTrustedSkis();
        assertThat(testManagement.getTrustedSkis().size(), is(0));
    }

    @Test
    public void test_authenticated_SKI() {
        testManagement.addTrustedSki(testSki, 32);
        assertThat(
            testManagement.getTrustedSkis().get(testSki).isAuthenticated(),
            is(false)
        );
        testManagement.addTrustedSki(testSki2, 64);
        assertThat(
            testManagement.getTrustedSkis().get(testSki2).isAuthenticated(),
            is(false)
        );

        testManagement.setTrustedSkiAuthenticated(testSki);
        assertThat(
            testManagement.getTrustedSkis().get(testSki).isAuthenticated(),
            is(true)
        );
    }

    @Test
    public void test_cipher_suite() {
        CertificateInfo certInfo = testManagement.getCert();
        assertThat(certInfo, is(not(nullValue())));
        assertThat(certInfo.certificate, is(not(nullValue())));
        assertThat(certInfo.privateKey, is(not(nullValue())));
        assertThat(certInfo.certificate.getType(), is("X.509"));
        assertThat(
            certInfo.certificate.getSigAlgName().toUpperCase(),
            is("SHA256WITHECDSA")
        );
    }
}
