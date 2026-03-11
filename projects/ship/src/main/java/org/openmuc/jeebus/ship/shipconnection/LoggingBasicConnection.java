/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.shipconnection;

import org.openmuc.jeebus.ship.message.MessageUtility;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class LoggingBasicConnection implements BasicConnection {
    private final Supplier<String> logPrefix;
    private final Logger log;
    private final BasicConnection delegate;

    public LoggingBasicConnection(
        Supplier<String> logPrefix,
        Logger log, BasicConnection delegate) {
        this.logPrefix = logPrefix;
        this.log = log;
        this.delegate = delegate;
    }

    @Override
    public void sendMsg(byte[] msg) {
        if (log.isDebugEnabled()) {
            log.debug(
                "{} sending message:\n{}",
                logPrefix.get(),
                MessageUtility.parseShipMsgToString(msg)
            );
        }
        delegate.sendMsg(msg);
    }

    @Override
    public void close() {
        log.info("{} closing connection", logPrefix.get());
        delegate.close();
    }
}
