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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;

/**
 * Certificate storage that stores the certificate in java keychain files.
 */
public class KeyStoreCertificateStorage implements CertificateStorage {
    protected static final Logger log = LoggerFactory.getLogger(KeyStoreCertificateStorage.class);

    private final String pathToKeyStore;
    private final String alias;
    private final char[] keyStorePassphrase;
    private final char[] keyPairPassphrase;

    /**
     * Creates a certificate storage that reads and stores the certificates in a java
     * keystore file.
     *
     * @param pathToKeyStore
     *     path where the key store exists or should be created
     * @param alias
     *     the alias for either the existing key pair or for the key pair to be
     *     created
     * @param keyStorePassphrase
     *     passphrase for the key store
     * @param keyPairPassphrase
     *     passphrase for the key pair to be generated
     */
    public KeyStoreCertificateStorage(
        String pathToKeyStore,
        String alias,
        char[] keyStorePassphrase,
        char[] keyPairPassphrase
    ) {
        Objects.requireNonNull(pathToKeyStore);
        Objects.requireNonNull(alias);

        this.pathToKeyStore = pathToKeyStore;
        this.alias = alias;
        this.keyStorePassphrase = keyStorePassphrase;
        this.keyPairPassphrase = keyPairPassphrase;
    }

    public String getPathToKeyStore() {
        return this.pathToKeyStore;
    }

    public String getAlias() {
        return this.alias;
    }

    public char[] getKeyStorePassphrase() {
        return this.keyStorePassphrase;
    }

    public char[] getKeyPairPassphrase() {
        return this.keyPairPassphrase;
    }

    @Override
    public Optional<CertificateInfo> readCertificate()
        throws CertificateStoreException
    {
        if (!this.doesKeyStoreExist()) {
            return Optional.empty();
        }

        try {
            KeyStore keyStore = this.loadKeyStore();
            if (!keyStore.containsAlias(this.alias)) {
                log.info("No certificate was found for alias {}.", alias);
                return Optional.empty();
            }

            Certificate certificate = keyStore.getCertificate(this.alias);
            Key key = keyStore.getKey(this.alias, this.keyPairPassphrase);

            if (!(key instanceof PrivateKey)) {
                throw new IllegalArgumentException(
                    "The key stored in the specified key store does not contain a"
                        + " valid private key under the given alias " + this.alias
                );
            }

            return Optional.of(new CertificateInfo(
                (PrivateKey) key,
                (X509Certificate) certificate
            ));
        } catch (Exception ex) {
            throw new CertificateStoreException("Failed to load key from keystore " + this.pathToKeyStore, ex);
        }
    }

    @Override
    public void saveCertificate(CertificateInfo certificate)
        throws CertificateStoreException
    {
        try {
            var keyStore = this.loadOrCreateKeyStore();

            X509Certificate[] certChain = new X509Certificate[] { certificate.certificate };
            keyStore.setKeyEntry(alias, certificate.privateKey, this.keyPairPassphrase, certChain);

            try (
                FileOutputStream fos = new FileOutputStream(this.pathToKeyStore);
                // try-with-resource unlocks it automatically
                FileLock ignored = fos.getChannel().lock()
            ) {
                keyStore.store(fos, this.keyStorePassphrase);
            }
        } catch (Exception ex) {
            throw new CertificateStoreException("Exception while storing key pair in key store.", ex);
        }
    }

    @Override
    public String toString() {
        return "KeyStoreCertificateStorage{" +
            "pathToKeyStore=" + this.pathToKeyStore +
            ", alias=" + this.alias +
            // Maybe not output any passwords...
            // ", keyStorePassphrase=" + Arrays.toString(keyStorePassphrase) +
            // ", keyPairPassphrase=" + Arrays.toString(keyPairPassphrase) +
            '}';
    }

    private KeyStore loadOrCreateKeyStore() throws
        KeyStoreException,
        NoSuchProviderException,
        CertificateException,
        IOException,
        NoSuchAlgorithmException
    {
        if (!this.doesKeyStoreExist()) {
            log.info(
                "No keystore has been found under path {}. Creating a new one.",
                pathToKeyStore
            );
            return this.createNewKeyStore();
        }

        return this.loadKeyStore();
    }

    private boolean doesKeyStoreExist() {
        return Files.exists(Path.of(this.pathToKeyStore));
    }

    private KeyStore loadKeyStore() throws
        KeyStoreException, NoSuchProviderException, IOException,
        CertificateException, NoSuchAlgorithmException
    {
        // For reference: https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#keystore-types
        KeyStore result = KeyStore.getInstance("pkcs12", "BC");

        try (
            FileInputStream fis = new FileInputStream(pathToKeyStore);
            FileLock ignored = fis.getChannel().lock(0, Long.MAX_VALUE, true)
        ) {
            result.load(fis, this.keyStorePassphrase);
        }
        return result;
    }

    private KeyStore createNewKeyStore() throws
        KeyStoreException, NoSuchProviderException, CertificateException,
        IOException, NoSuchAlgorithmException
    {
        KeyStore result = KeyStore.getInstance("pkcs12", "BC");
        result.load(null, this.keyStorePassphrase);
        return result;
    }
}
