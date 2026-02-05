/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.machine;

import org.openmuc.jeebus.ship.node.StaticConfiguration;
import org.openmuc.jeebus.ship.view.UserInterface;

import java.util.concurrent.TimeUnit;

/**
 * This interface encapsulates all the ways in which a {@link StateHandler} may
 * interact with the network, the state machine, and the various timers.
 */
public interface StateHandlerContext {
    /**
     * @return the current state. Mostly useful for handlers that handle multiple states.
     */
    State getState();

    /**
     * Transition the state machine.
     * @param next which state to enter now
     */
    void transitionTo(State next);

    /**
     * Enable Connection Data Exchange (it cannot be disabled afterward)
     */
    void enableConnectionDataExchange();

    /**
     * Close the SPINE connection with no further messaging.
     */
    void closeConnection();

    /**
     * Send a message to the communication partner.
     * @param msg the message to send
     */
    void sendMessage(byte[] msg);

    /**
     * Indicate that the handler is ready to process a waiting message from the
     * communication partner.
     */
    void processMessage();

    /**
     * Start a specific timeout with the default duration.
     * @param which which timeout to start.
     */
    void startTimeout(SpecifiedTimeout which);

    /**
     * Start a specific timeout with the given duration.
     * @param which which timeout to start
     * @param seconds how long it should run
     */
    default void startTimeout(SpecifiedTimeout which, int seconds) {
        startTimeout(which, seconds, TimeUnit.SECONDS);
    }

    /**
     * Start a specific timeout with the given duration.
     * @param which which timeout to start
     * @param amount how long it should run
     * @param unit the unit for {@code amount}
     */
    void startTimeout(SpecifiedTimeout which, int amount, TimeUnit unit);

    /**
     * Prolong a specific timeout by the initial value.
     *
     * @param which
     *     which timeout to prolong
     */
    void increaseTimeout(SpecifiedTimeout which);

    /**
     * Prolong a specific timeout.
     * @param which which timeout to prolong
     * @param extraSeconds how many seconds to add
     */
    void increaseTimeout(SpecifiedTimeout which, int extraSeconds);

    /**
     * Stop one or more timeouts.
     * @param which which timeout(s) to stop.
     */
    void stopTimeouts(SpecifiedTimeout... which);

    /**
     * Stop all timeouts.
     */
    default void stopAllTimeouts() {
        stopTimeouts(SpecifiedTimeout.values());
    }

    /**
     * Query the status of a specific timeout.
     * @param which which timeout to get the status of
     * @return time remaining (in seconds), or null if not active.
     */
    Integer getTimeoutStatus(SpecifiedTimeout which);

    /**
     * @return whether the caller is on the server side of a connection.
     */
    boolean isServerSide();

    /**
     * @param minimumTrust required trust level
     * @return whether the communication partner is trusted at least this much
     */
    boolean requireTrust(int minimumTrust);

    /**
     * @return the configuration for this node
     */
    StaticConfiguration getConfig();

    /**
     * @return the interface to prompt the user about some action
     */
    UserInterface getUserInterface();

    /**
     * Store some arbitrary extra data.
     * <p>
     * The handler must be marked with a
     * {@link StateHandler.UsesExtraData} annotation indicating the type of
     * the extra data it uses.
     * @param data the data to store.
     */
    void setExtraData(Object data);

    /**
     * Retrieve data previously stored with {@link #setExtraData(Object)}.
     * <p>
     * The handler must be marked with a
     * {@link StateHandler.UsesExtraData} annotation indicating the type of
     * the extra data it uses.
     * @param dataClass class of the data to retrieve
     * @return stored data, or null if not present.
     * @param <T> type of the data to retrieve
     */
    <T> T getExtraData(Class<T> dataClass);

    void setPeerSkiAuthenticated();
}
