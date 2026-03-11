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

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.node.websocket.SkiManagementInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * everything related to key management and key encryption
 */
public class KeyManagement {

    protected static final Logger log = LoggerFactory.getLogger(KeyManagement.class);
    private static final Map<String, SkiManagementInfo> trustedSkis
        = new ConcurrentHashMap<>();
    private final KeyPair keyPair;
    private final String pathToKeyStore;
    private final CertificateInfo cert;
    private final char[] keyStorePass;
    private final char[] keyPairPass;
    private final SubjectKeyIdentifier ownSki;

    /**
     * Creates a new key store, key pair and certificate and stores it in the
     * specified path. Should the path already contain a key store with a valid key
     * pair and certificate, then the key store will be loaded instead.
     *
     * @param pathToKeyStore
     *     path where the key store exists or should be created, a temporary key
     *     store will be created and will not be stored should the path be
     *     <code>null</code>
     * @param alias
     *     the alias for either the existing key pair or for the key pair to be
     *     created
     * @param keyStorePassphrase
     *     passphrase for the key store
     * @param keyPairPassphrase
     *     passphrase for the key pair to be generated
     * @param distinguishedName
     *     X.509 Distinguished Name, eg "CN=Test, L=London, C=GB". For IoT devices,
     *     usually the DeviceID
     * @param days
     *     how many days the certificate should be valid for
     * @throws KeyStoreException
     *     if a KeyStoreSpi implementation for the specified type is not available
     *     from the specified provider
     * @throws NoSuchProviderException
     *     if the specified provider is not registered in the security provider list
     * @throws IOException
     *     if there is an I/O or format problem with the keystore data, if a password
     *     is required but not given, or if the given password was incorrect
     * @throws CertificateException
     *     if any of the certificates in the keystore could not be loaded
     * @throws NoSuchAlgorithmException
     *     if the appropriate data integrity algorithm could not be found
     * @throws UnrecoverableKeyException
     *     if the key cannot be recovered (e.g., the given password is wrong).
     */
    public KeyManagement(
        String pathToKeyStore,
        String alias,
        char[] keyStorePassphrase,
        char[] keyPairPassphrase,
        String distinguishedName,
        int days
    ) throws
        KeyStoreException,
        NoSuchProviderException,
        IOException,
        CertificateException,
        NoSuchAlgorithmException,
        UnrecoverableKeyException
    {
        Objects.requireNonNull(alias);
        this.pathToKeyStore = pathToKeyStore;
        this.keyStorePass = keyStorePassphrase;
        this.keyPairPass = keyPairPassphrase;
        addBCProvider();
        KeyStore keyStore = loadKeyStore();
        if (keyStore.containsAlias(alias)) {
            Certificate certificate = keyStore.getCertificate(alias);

            Key key = keyStore.getKey(alias, keyPairPassphrase);
            if (key instanceof PrivateKey) {
                keyPair = new KeyPair(certificate.getPublicKey(), (PrivateKey) key);
                ownSki = generateSki(certificate.getPublicKey());
                cert = new CertificateInfo(
                    (PrivateKey) key,
                    (X509Certificate) certificate
                );
            }
            else {
                throw new IllegalArgumentException(
                    "The key stored in the specified key store does not contain a"
                        + " valid private key under the given alias"
                );
            }
        }
        else {
            log.info(
                "No certificate was found for alias {}. Creating a new one.",
                alias
            );
            keyPair = createKeyPair();
            ownSki = generateSki(keyPair.getPublic());
            cert = createCertificate(keyPair, distinguishedName, days, null);
            storeCertificate(alias);
        }
    }

    private boolean keyStoreExists() {
        return pathToKeyStore != null && Files.exists(Path.of(pathToKeyStore));
    }

    public static SubjectKeyIdentifier generateSki(PublicKey publicKey) {
        JcaX509ExtensionUtils utils = null;
        try {
            utils = new JcaX509ExtensionUtils();
        }
        catch (NoSuchAlgorithmException e) {
            log.error("exception while generating SKI value: ", e);
        }
        assert utils != null;
        return utils.createSubjectKeyIdentifier(publicKey);
    }

    /**
     * returns the hex string that represents the SKI value
     *
     * @param ski
     *     SubjectKeyIdentifier value
     * @return SKI value as hex string in lower case
     */
    public static String encodeSkiAsString(SubjectKeyIdentifier ski) {
        return Hex.toHexString(ski.getKeyIdentifier()).toLowerCase();
    }

    /**
     * checks if a given string represents a valid SKI. Note that this method removes
     * whitespaces prior to checking
     *
     * @param ski
     *     the string to check
     * @return <code>true</code> if the string only uses hex digits and has a length
     * of exactly 40
     */
    public static boolean isValidSki(String ski) {
        return MessageUtility.isHexDigits(ski)
            && ski.replaceAll("\\s+", "").length() == 40;
    }

    /**
     * adds provider only if it's not already in the JVM
     */
    public static void addBCProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * adds the given SKI to the trusted list if it was not added already. This
     * method does not override previous entries. For an override remove the previous
     * entry with {@link #removeTrustedSki(String) removeTrustedSki} before adding it
     * again with the new trust level.
     *
     * @param ski
     *     the SKI to be added to the trusted SKIs list
     * @param trustLevel
     *     the trust level of the SKI to be added
     */
    public static void addTrustedSki(String ski, Integer trustLevel) {
        if (!isValidSki(ski)) {
            throw new IllegalArgumentException("SKI is invalid");
        }
        if (trustLevel < 8 || trustLevel > 96) {
            throw new IllegalArgumentException(
                "trust level should be a value between 8 and 96");
        }
        if (trustedSkis.containsKey(ski)) {
            log.warn("The SKI {} is already in the list of trusted SKIs", ski);
        }
        else {
            trustedSkis.put(ski, new SkiManagementInfo(trustLevel));
            log.info("added the SKI {} to the list of trusted SKIs", ski);
        }
    }

    public static void setTrustedSkiAuthenticated(String ski) {
        if (trustedSkis.containsKey(ski)) {
            trustedSkis.get(ski).setAuthenticated(true);
        }
        else {
            throw new IllegalArgumentException(
                "SKI to authenticate should be in the trusted SKIs list");
        }
    }

    /**
     * removes a ski from the trustedSkis map
     *
     * @param ski
     *     the ski to remove
     * @return <code>true</code> if the map contained the ski, otherwise
     * <code>false</code>
     */
    public static boolean removeTrustedSki(String ski) {
        if (ski == null) {
            return false;
        }
        return trustedSkis.remove(ski) != null;
    }

    public static Map<String, SkiManagementInfo> getTrustedSkis() {
        return trustedSkis;
    }

    public static void clearTrustedSkis() {
        trustedSkis.clear();
    }

    private void storeCertificate(
        String alias
    ) throws
        IOException,
        CertificateException,
        KeyStoreException,
        NoSuchAlgorithmException,
        NoSuchProviderException
    {
        KeyStore keyStore = loadKeyStore();
        try {
            X509Certificate[] certChain = new X509Certificate[] { cert.certificate };
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), keyPairPass, certChain);
        }
        catch (Exception e) {
            log.error("Exception while storing key pair in key store.", e);
        }

        if (pathToKeyStore != null) {
            try (
                FileOutputStream fos = new FileOutputStream(pathToKeyStore);
                // try-with-resource unlocks it automatically
                FileLock ignored = fos.getChannel().lock()
            ) {
                keyStore.store(fos, keyStorePass);
            }
        }
    }

    private KeyStore loadKeyStore() throws
        KeyStoreException,
        NoSuchProviderException,
        CertificateException,
        IOException,
        NoSuchAlgorithmException
    {
        // For reference: https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#keystore-types
        KeyStore result = KeyStore.getInstance("pkcs12", "BC");
        if (keyStoreExists()) {
            try (
                FileInputStream fis = new FileInputStream(pathToKeyStore);
                FileLock ignored = fis.getChannel().lock(0, Long.MAX_VALUE, true)
            ) {
                result.load(fis, keyStorePass);
            }
        }
        else {
            if (pathToKeyStore != null) {
                log.info(
                    "No keystore has been found under path {}. Creating a new one.",
                    pathToKeyStore
                );
            }
            result.load(null, keyStorePass);
        }
        return result;
    }

    /**
     * We are not sure what the symmetric key methods are for.
     * Maybe this logic concerns SHIP 1.1.0.
     * As they are used nowhere right now, they may change or be removed in the
     * future.
     * <p>
     * TODO: figure this out.
     */
    @Deprecated
    public void storeSymKeyInKeyStore(String alias) throws
        NoSuchProviderException,
        KeyStoreException,
        CertificateException,
        NoSuchAlgorithmException,
        IOException
    {
        KeyStore specialKeyStore = KeyStore.getInstance("BKS", "BC");
        if (keyStoreExists()) {
            specialKeyStore.load(new FileInputStream(pathToKeyStore), keyStorePass);
        }
        else {
            specialKeyStore.load(null, null);
        }
        // wrap the symmetric key
        SecretKeySpec symmetricKey = createSymmetricKey();
        KeyStore.SecretKeyEntry secret = new KeyStore.SecretKeyEntry(symmetricKey);
        KeyStore.ProtectionParameter pwd = new KeyStore.PasswordProtection(
            keyStorePass
        );
        specialKeyStore.setEntry(alias, secret, pwd);
        // save the keystore file
        try (FileOutputStream fos = new FileOutputStream(pathToKeyStore)) {
            specialKeyStore.store(fos, keyStorePass);
        }
    }

    /**
     * Generates an asymmetric ECDHE key pair
     *
     * @return A new asymmetric EC key pair
     */
    private static KeyPair createKeyPair() {
        KeyPairGenerator gen;
        try {
            gen = KeyPairGenerator.getInstance("EC", "BC");
            ECGenParameterSpec eccCurve = new ECGenParameterSpec("secp256r1");
            gen.initialize(eccCurve);
        }
        catch (
            NoSuchAlgorithmException |
            NoSuchProviderException |
            InvalidAlgorithmParameterException e
        ) {
            throw new RuntimeException("Exception while generating EC KeyPair", e);
        }
        return gen.generateKeyPair();
    }

    /**
     * @see KeyManagement#storeSymKeyInKeyStore
     */
    @Deprecated
    private SecretKeySpec createSymmetricKey() {
        byte[] keyBytes = new byte[16];
        new SecureRandom().nextBytes(keyBytes);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * generates a self-signed X.509 Certificate
     *
     * @param keyPair
     *     passphrase for the key pair to be generated
     * @param distinguishedName
     *     the X.509 Distinguished Name, eg "CN=TEst, L=London, C=GB". For IoT
     *     devices, usually the DeviceID
     * @param days
     *     how many days the Certificate is valid for
     * @param issuer
     *     certificate content, consists of privateKey and X509Certificate
     * @return A self-signed certificate
     */
    public CertificateInfo createCertificate(
        KeyPair keyPair,
        String distinguishedName,
        int days,
        CertificateInfo issuer
    ) {
        addBCProvider();

        X500Name dnName = new X500Name(distinguishedName);
        BigInteger certSerialNumber = BigInteger.valueOf(System.currentTimeMillis());

        Instant validFrom = Instant.now();
        Instant validUntil = validFrom.plus(days, ChronoUnit.DAYS);

        X500Name issuerName;
        PrivateKey issuerKey;
        // if issuer is null, self-sign certificate
        if (issuer == null) {
            issuerName = dnName;
            issuerKey = keyPair.getPrivate();
        }
        else {
            issuerName = new X500Name(issuer.certificate.getSubjectDN().getName());
            issuerKey = issuer.privateKey;
        }

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuerName,
            certSerialNumber,
            Date.from(validFrom),
            Date.from(validUntil),
            dnName,
            keyPair.getPublic()
        );

        X509Certificate cert;
        try {
            // Add SubjectKeyIdentifier (SKI) to certificate
            certBuilder.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                this.ownSki
            );

            // Make the cert builder a cert authority in case more certs are needed
            certBuilder.addExtension(
                Extension.basicConstraints,
                true, // Basic Constraints is usually marked as critical.
                new BasicConstraints(true)
            );

            ASN1Encodable[] subjectAlternativeNames = new ASN1Encodable[] {
                new GeneralName(
                    GeneralName.dNSName,
                    "IP Address=" + distinguishedName
                )
            };
            certBuilder.addExtension(
                Extension.subjectAlternativeName,
                false,
                new DERSequence(subjectAlternativeNames)
            );
            // sign the certificate
            String signatureAlgorithm = "SHA256WithECDSA";
            ContentSigner signer= new JcaContentSignerBuilder(signatureAlgorithm)
                .build(issuerKey);
            X509CertificateHolder certHolder = certBuilder.build(signer);
            cert = new JcaX509CertificateConverter()
                .getCertificate(certHolder);
        }
        catch (
            OperatorCreationException
            | CertIOException
            | CertificateException e
        ) {
            /* A certificate is vital for any SHIP-communication.
             * So if we fail to create a certificate, starting SHIP makes no sense.
             * Thus, we want to fail the program early with a RuntimeException
             * or have it handled somewhere. */
            throw new RuntimeException(
                "Exception while creating new certificate.",
                e
            );
        }
        return new CertificateInfo(keyPair.getPrivate(), cert);
    }

    public SubjectKeyIdentifier getOwnSki() {
        return this.ownSki;
    }

    public String getOwnSkiAsStr() {
        return encodeSkiAsString(getOwnSki());
    }

    public CertificateInfo getCert() {
        return cert;
    }

}
