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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A {@code StateHandler} is a collection of event handlers associated with a given
 * SHIP connection state. Its methods will be called by the state machine if it is
 * in the appropriate state and the corresponding event occurs.
 * <p>
 * Any class that implements this interface should be totally stateless, i.e. should
 * have no non-final fields and no fields that contain mutable objects. Instances
 * also should not directly start timers, communicate over the network, etc. Instead,
 * these should be done via the {@link StateHandlerContext} passed to each method.
 */
public interface StateHandler {

    Logger LOGGER = LoggerFactory.getLogger(StateHandler.class);

    /**
     * Called when the state machine enters this state.
     * <p>
     * Invoking {@link StateHandlerContext#transitionTo(State)} inside this method
     * will defer the transition until {@code onEntered} finishes.
     * @param previous the previous state
     * @param context the execution context
     */
    default void onEntered(State previous, StateHandlerContext context) {}

    /**
     * Called just before the state machine transitions to a next state.
     * <p>
     * Invoking {@link StateHandlerContext#transitionTo(State)} inside this method
     * is an error.
     * @param pendingNext the state that will be entered
     * @param context the execution context
     */
    default void onBeforeExit(State pendingNext, StateHandlerContext context) {}

    /**
     * Called when a message was received from the communication partner.
     * <p>
     * If you wish to evaluate the message, the returned command should contain
     * {@link StateHandlerContext#processMessage()}, which will cause
     * {@link #processMessage(byte[], StateHandlerContext)}
     * to be called with the message contents.
     * @param context the execution context
     */
    default void onMessageReceived(StateHandlerContext context) {}

    /**
     * Called after {@link StateHandlerContext#processMessage()}.
     * @param msg the raw message bytes
     * @param context the execution context
     */
    default void processMessage(byte[] msg, StateHandlerContext context) {
        throw new UnsupportedOperationException();
    }

    /**
     * Called when a previously started timeout expires.
     * @param kind which timeout expired
     */
    default void onTimeoutExpired(
        SpecifiedTimeout kind,
        StateHandlerContext context
    ) {
        LOGGER.warn(
            "unhandled onTimeoutExpired: kind={}, state={}",
            kind,
            context.getState()
        );
    }

    /**
     * Indicates which state(s) this handler takes care of.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface Handles {
        State[] value();
    }

    /**
     * Indicates that this state needs some extra data of the specified type.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface UsesExtraData {
        Class<?> value();
    }

    default Class<?> getExtraDataType() {
        UsesExtraData annotation = getClass().getAnnotation(UsesExtraData.class);
        return annotation == null
            ? null
            : annotation.value();
    }

    /**
     * Placeholder value.
     */
    StateHandler UNIMPLEMENTED = new StateHandler() {
        @Override
        public void onEntered(State previous, StateHandlerContext context) {
            throw new UnsupportedOperationException(
                "entered unimplemented state from previous state " + previous);
        }
    };
}
