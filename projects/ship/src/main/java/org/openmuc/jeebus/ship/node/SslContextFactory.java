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

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.openmuc.jeebus.ship.api.cert.CertificateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import java.util.List;

public class SslContextFactory {
    private static final Logger log
        = LoggerFactory.getLogger(SslContextFactory.class);

    private final List<String> PROTOCOLS;

    private final List<String> CIPHERS;

    private SslContext sslContext;

    private TrustManager[] trustManagers;

    public SslContextFactory() {
        this.PROTOCOLS = StaticConfiguration.getProtocols();
        this.CIPHERS = StaticConfiguration.getCiphers();
    }

    public SslContext generateServerSslContext(CertificateInfo cert) throws
        SSLException {
        log.debug("generating server-side SSL context");
        return generateSslContext(SslContextBuilder.forServer(
            cert.privateKey,
            cert.certificate
        ));
    }

    public SslContext generateClientSslContext(CertificateInfo cert) throws
        SSLException {
        log.debug("generating client-side SSL context");
        return generateSslContext(SslContextBuilder
            .forClient()
            .keyManager(cert.privateKey, cert.certificate));
    }

    private SslContext generateSslContext(SslContextBuilder sslContextBuilder) throws
        SSLException {
        return sslContext = sslContextBuilder.protocols(PROTOCOLS)
            .ciphers(CIPHERS)
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .clientAuth(ClientAuth.REQUIRE)
            .build();
    }

    public TrustManager[] getTrustManagers() {
        return trustManagers;
    }

    public void setTrustManager(TrustManager[] trustManagers) {
        this.trustManagers = trustManagers;
    }

    public SslContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SslContext sslContext) {
        this.sslContext = sslContext;
    }
}
