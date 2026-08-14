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
import org.openmuc.jeebus.ship.api.cert.*;
import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.node.websocket.SkiManagementInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * everything related to key management and key encryption
 */
public class KeyManagement {

    protected static final Logger log = LoggerFactory.getLogger(KeyManagement.class);
    private final Map<String, SkiManagementInfo> trustedSkis
        = new ConcurrentHashMap<>();
    private final CertificateInfo cert;
    private final SubjectKeyIdentifier ownSki;

    /**
     * Loads the key by using the provided certificateStorage.
     * If no key is found, a new key is created.
     *
     * @param certificateStorage Certificate storage used for loading and storing the
     *     key
     * @param distinguishedName
     *     X.509 Distinguished Name, eg "CN=Test, L=London, C=GB". For IoT devices,
     *     usually the DeviceID
     * @param shipId
     *     the SHIP ID of this node is used as a Subject Alternative Name
     *     in the certificate
     * @param days
     *     how many days the certificate should be valid for
     * @throws CertificateStoreException
     *     if the certificate storage implementation fails to load the key
     */
    public KeyManagement(
        CertificateStorage certificateStorage,
        String distinguishedName,
        String shipId,
        int days
    ) throws CertificateStoreException
    {
        addBCProvider();

        Optional<CertificateInfo> storedCert = certificateStorage.readCertificate();
        if (storedCert.isPresent()) {
            this.cert = storedCert.get();
        } else {
            this.cert = this.createCertificate(
                createKeyPair(),
                distinguishedName,
                shipId,
                days,
                null
            );
            certificateStorage.saveCertificate(this.cert);
        }

        this.ownSki = generateSki(this.cert.certificate.getPublicKey());
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
     * @return {@code true} if the string only uses hex digits and has a length
     * of exactly 40
     */
    public static boolean isValidSki(String ski) {
        return ski != null
            && !ski.isBlank()
            && MessageUtility.isHexDigits(ski)
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
    public void addTrustedSki(String ski, Integer trustLevel) {
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

    public void setTrustedSkiAuthenticated(String ski) {
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
     * @return {@code true} if the map contained the ski, otherwise
     * {@code false}
     */
    public boolean removeTrustedSki(String ski) {
        if (ski == null) {
            return false;
        }
        return trustedSkis.remove(ski) != null;
    }

    /**
     * @return an unmodifiable view of the trusted SKI Map
     */
    public Map<String, SkiManagementInfo> getTrustedSkis() {
        return Collections.unmodifiableMap(trustedSkis);
    }

    public void clearTrustedSkis() {
        trustedSkis.clear();
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
     * generates a self-signed X.509 Certificate
     *
     * @param keyPair
     *     passphrase for the key pair to be generated
     * @param distinguishedName
     *     the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB". For IoT
     *     devices, usually the DeviceID
     * @param shipId
     *     the SHIP ID of this node is used as a Subject Alternative Name
     *     in the certificate
     * @param days
     *     how many days the Certificate is valid for
     * @param issuer
     *     certificate content, consists of privateKey and X509Certificate
     * @return A self-signed certificate
     */
    public CertificateInfo createCertificate(
        KeyPair keyPair,
        String distinguishedName,
        String shipId,
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
                generateSki(keyPair.getPublic())
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
                    shipId
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
