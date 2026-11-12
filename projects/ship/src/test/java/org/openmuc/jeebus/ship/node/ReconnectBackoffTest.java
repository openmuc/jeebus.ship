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

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class ReconnectBackoffTest {

    private static final long INITIAL = 1000;
    private static final long MAX = 60000;
    private static final double FACTOR = 2.0;
    private static final double JITTER = 0.2;
    /**
     * Tolerance for the jitter bounds, so that double rounding and the
     * truncation to long cannot produce off-by-one failures.
     */
    private static final long TOLERANCE = 1;

    @Test
    public void attemptZeroIsWithinJitterBounds() {
        long delay = ReconnectBackoff.delayMillis(
            0, INITIAL, MAX, FACTOR, JITTER, new Random(42)
        );

        assertThat(delay, is(greaterThanOrEqualTo(lowerBound(INITIAL))));
        assertThat(delay, is(lessThanOrEqualTo(upperBound(INITIAL))));
    }

    @Test
    public void delayGrowsExponentiallyWithoutJitter() {
        long attempt0 = ReconnectBackoff.delayMillis(
            0, INITIAL, MAX, FACTOR, 0.0, new Random(1)
        );
        long attempt1 = ReconnectBackoff.delayMillis(
            1, INITIAL, MAX, FACTOR, 0.0, new Random(1)
        );
        long attempt2 = ReconnectBackoff.delayMillis(
            2, INITIAL, MAX, FACTOR, 0.0, new Random(1)
        );

        assertThat(attempt0, is(INITIAL));
        assertThat(attempt1, is(2 * INITIAL));
        assertThat(attempt2, is(4 * INITIAL));
        assertThat(attempt1, is(greaterThan(attempt0)));
        assertThat(attempt2, is(greaterThan(attempt1)));
    }

    @Test
    public void hugeAttemptIsCappedAtMaxDelay() {
        long delay = ReconnectBackoff.delayMillis(
            1000, INITIAL, MAX, FACTOR, JITTER, new Random(7)
        );

        assertThat(delay, is(MAX));
    }

    @Test
    public void jitterStaysWithinTwentyPercent() {
        Random random = new Random(1234);

        for (int attempt = 0; attempt < 5; attempt++) {
            double base = INITIAL * Math.pow(FACTOR, attempt);
            for (int i = 0; i < 100; i++) {
                long delay = ReconnectBackoff.delayMillis(
                    attempt, INITIAL, MAX, FACTOR, JITTER, random
                );

                assertThat(delay, is(greaterThanOrEqualTo(lowerBound(base))));
                assertThat(delay, is(lessThanOrEqualTo(upperBound(base))));
            }
        }
    }

    @Test
    public void zero_initial_delay_returns_zero() {
        long delay = ReconnectBackoff.delayMillis(
            3, 0, MAX, FACTOR, JITTER, new Random(9)
        );

        assertThat(delay, is(0L));
    }

    @Test
    public void negativeAttemptBehavesLikeAttemptZero() {
        long negative = ReconnectBackoff.delayMillis(
            -5, INITIAL, MAX, FACTOR, JITTER, new Random(99)
        );
        long zero = ReconnectBackoff.delayMillis(
            0, INITIAL, MAX, FACTOR, JITTER, new Random(99)
        );

        assertThat(negative, is(zero));
    }

    @Test
    public void delayIsNeverNegativeAndNeverAboveMax() {
        Random random = new Random(2024);

        for (int attempt = 0; attempt < 40; attempt++) {
            long delay = ReconnectBackoff.delayMillis(
                attempt, INITIAL, MAX, FACTOR, JITTER, random
            );

            assertThat(delay, is(greaterThanOrEqualTo(0L)));
            assertThat(delay, is(lessThanOrEqualTo(MAX)));
        }
    }

    private static long lowerBound(double base) {
        return (long) (base * (1 - JITTER)) - TOLERANCE;
    }

    private static long upperBound(double base) {
        return (long) (base * (1 + JITTER)) + TOLERANCE;
    }
}
