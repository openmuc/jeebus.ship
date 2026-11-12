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

import java.util.Random;

/**
 * Computes the delay before the next reconnection attempt.
 *
 * <p>The delay grows exponentially with the number of failed attempts and is
 * capped at a configurable maximum, as suggested by SHIP:13.4.7.1.2. A relative
 * jitter is applied so that many devices which lost the same peer do not
 * reconnect in lockstep and flood the network (thundering herd).</p>
 *
 * <p>This class is stateless: it only maps an attempt counter and the
 * configured parameters to a delay. The attempt counter itself is kept by the
 * caller.</p>
 */
public final class ReconnectBackoff {

    private ReconnectBackoff() {
        // only static methods
    }

    /**
     * Computes the delay before the next reconnection attempt.
     *
     * <p>The base delay is {@code initialDelayMillis * factor^attempt}. If that
     * value reaches or exceeds {@code maxDelayMillis}, the maximum is returned
     * unchanged, i.e. the cap is a hard bound and is never jittered. Otherwise
     * the base delay is jittered by up to {@code ±jitter} (relative) and clamped
     * into {@code [0, maxDelayMillis]}.</p>
     *
     * <p>Attempt 0 is jittered as well, so the first delay is only approximately
     * {@code initialDelayMillis}.</p>
     *
     * @param attempt            number of failed attempts so far; negative values
     *                           are treated as 0
     * @param initialDelayMillis base delay of the first attempt in milliseconds
     * @param maxDelayMillis     upper bound in milliseconds
     * @param factor             growth factor per attempt, typically 2.0
     * @param jitter             relative jitter, e.g. 0.2 for ±20 %
     * @param random             source of randomness, injected for testability
     * @return the delay in milliseconds, never negative and never above
     *         {@code maxDelayMillis}
     */
    public static long delayMillis(
        int attempt,
        long initialDelayMillis,
        long maxDelayMillis,
        double factor,
        double jitter,
        Random random
    ) {
        if (initialDelayMillis <= 0 || maxDelayMillis <= 0) {
            return 0;
        }

        double baseDelay
            = initialDelayMillis * Math.pow(factor, Math.max(attempt, 0));
        if (!Double.isFinite(baseDelay) || baseDelay >= maxDelayMillis) {
            return maxDelayMillis;
        }

        double jitterFactor = 1 + jitter * (2 * random.nextDouble() - 1);
        double jitteredDelay = baseDelay * jitterFactor;
        return (long) Math.min(Math.max(jitteredDelay, 0), maxDelayMillis);
    }
}
