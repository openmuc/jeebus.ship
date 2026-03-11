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

import org.openmuc.jeebus.ship.node.Configuration;
import org.openmuc.jeebus.ship.node.KeyManagement;
import org.openmuc.jeebus.ship.shipconnection.ShipConnection;
import org.openmuc.jeebus.ship.view.UserInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * SHIP state machine, handles connection setup until Connection Data Exchange is reached.
 */
public class StateMachine implements StateHandlerContext {
    private static final Logger LOGGER
        = LoggerFactory.getLogger(StateMachine.class);
    protected State state;
    protected StateHandler actions;
    // "escape hatch" for states to store auxiliary extra data, e.g. last sent
    // message
    protected Class<?> extraDataType;
    protected Object stateExtraData;

    // this ensures we correctly handle any number of transitionTo() call from inside
    // an onEntered / onBeforeExit.
    // (In truth, the Java call stack limits the maximum depth)
    private transient State pendingNext;
    private transient Microstate _microstate = Microstate.NORMAL;

    private enum Microstate {
        NORMAL,
        EXITING,
        ENTERING
    }

    private final boolean isServer;
    protected final ShipConnection shipConnection;
    private final UserInterface userInterface;
    private final Configuration config;
    private byte[] pendingMessage;

    protected ScheduledExecutorService timeoutExecutor
        = Executors.newSingleThreadScheduledExecutor();
    // invariant: extraDelays.keySet() is a subset of runningTimeouts.keySet()
    private final Map<SpecifiedTimeout, ScheduledFuture<?>> runningTimeouts
        = new EnumMap<>(SpecifiedTimeout.class);
    // extra delays to handle "prolong this timeout" operations
    private final Map<SpecifiedTimeout, Integer> extraDelays
        = new EnumMap<>(SpecifiedTimeout.class);
    // cache the `() -> timedOut(which)` lambdas so they don't get allocated for
    // every call to startTimeout() and timedOut()
    @SuppressWarnings("Convert2Diamond")  // doesn't actually work with <>
    private transient final Map<SpecifiedTimeout, Runnable> TIMED_OUT
        = Collections.unmodifiableMap(
        new EnumMap<SpecifiedTimeout, Runnable>(Arrays
            .stream(
                SpecifiedTimeout.values())
            .collect(Collectors.toMap(to -> to, to -> () -> timedOut(to)))));

    public StateMachine(
        ShipConnection shipConnection,
        UserInterface userInterface,
        Configuration config,
        State initial,
        Object extraData
    ) {
        this.shipConnection = shipConnection;
        this.userInterface = userInterface;

        this.config = config;
        this.isServer = shipConnection.isServer();
        state = initial;
        actions = state != null ? state.getHandler() : null;
        setNewExtraData(extraData);
    }

    public StateMachine(
        ShipConnection shipConnection,
        UserInterface userInterface,
        Configuration config
    ) {
        this(shipConnection, userInterface, config, null, null);
    }

    public void begin() {
        if (state != null) {
            throw new IllegalStateException(
                "Cannot begin an already running state machine!");
        }
        transitionTo(State.CMI_INIT_START);
    }

    @Override
    public synchronized void transitionTo(State newState) {
        // handle nested calls
        switch (_microstate) {
            case EXITING:
                throw new IllegalStateException("Cannot transition into a new state"
                    + "while exiting another state: " + state);
            case ENTERING:
                if (pendingNext != null) {
                    LOGGER.error(
                        "Overwriting pending next state: was {}, now {}.\n"
                            + "This is almost certainly a bug! Most likely it is the"
                            + " {} state that is misbehaving.",
                        pendingNext,
                        newState, state
                    );
                }
                pendingNext = newState;
                return;
            // default: case NORMAL
        }

        // sanity check: is the transition allowed?
        if (state != null && !state.getAllowedTransitions().contains(newState)) {
            throw new IllegalStateException(String.format(
                "Cannot transition to state %s from state %s", newState, state));
        }
        LOGGER.debug("{} --> {}", state, newState);

        // exit callback
        _microstate = Microstate.EXITING;
        if (actions != null) {
            actions.onBeforeExit(newState, this);
        }

        // actually swap states
        State old = swapState(newState);

        // enter callback
        _microstate = Microstate.ENTERING;
        pendingNext = null;
        actions.onEntered(old, this);
        _microstate = Microstate.NORMAL;

        // if there was a nested transitionTo(), handle that transition now
        if (pendingNext != null) {
            newState = pendingNext;
            pendingNext = null;
            transitionTo(newState);
        }
    }

    protected State swapState(State newState) {
        State old = state;
        state = newState;
        actions = newState.getHandler();
        // swap extra data
        setNewExtraData(null);
        return old;
    }

    private void setNewExtraData(Object newData) {
        extraDataType = actions != null ? actions.getExtraDataType() : null;
        if (extraDataType == null) {
            stateExtraData = null;
        }
        else if (!extraDataType.isInstance(stateExtraData)) {
            try {
                stateExtraData = newData == null
                    ? extraDataType.getConstructor().newInstance()
                    : newData;
            }
            catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void sendMessage(byte[] msg) {
        shipConnection.sendRawMessage(msg);
    }

    public synchronized void messageReceived(byte[] msg) {
        if (pendingMessage != null) {
            LOGGER.warn("Overwriting unprocessed pending message with new"
                + "incoming message!");
        }
        pendingMessage = msg;
        actions.onMessageReceived(this);
    }

    @Override
    public synchronized void processMessage() {
        if (pendingMessage == null) {
            throw new IllegalStateException(
                "processMessage() called with no pending message");
        }
        actions.processMessage(pendingMessage, this);
        pendingMessage = null;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void closeConnection() {
        shipConnection.closeImmediately();
    }

    public Configuration getConfig() {
        return config;
    }

    public int getDefaultTimeoutSeconds(SpecifiedTimeout which) {
        switch (which) {
            case CMI_TIMEOUT:
                return config.getCmiTimeoutVal();
            case SME_WAIT_FOR_READY:
                return config.getT_hello_init();
            case SME_SEND_PROLONGATION_REQUEST:
            case SME_PROLONGATION_REQUEST_REPLY:
                LOGGER.warn(
                    "Timer {} should never use default duration! Using 10 seconds "
                        + "as fallback",
                    which
                );
                return 10;
            case SME_PROTH_WAIT:
                return 10;
            default:
                LOGGER.warn("missing timeout length: {}", which);
                return -1;
        }
    }

    /**
     * Replace the executor used for scheduling timeouts (this is mainly useful for
     * testing). Cannot be called while there are already timeouts scheduled.
     *
     * @param executor
     *     the new executor to use
     */
    public void setTimeoutExecutor(ScheduledExecutorService executor) {
        synchronized (this) {
            if (!runningTimeouts.isEmpty()) {
                throw new IllegalStateException(
                    "cannot replace executor while timeouts are running!");
            }
            timeoutExecutor = executor;
        }
    }

    @Override
    public void stopTimeouts(SpecifiedTimeout... which) {
        synchronized (this) {
            for (SpecifiedTimeout to : which) {
                ScheduledFuture<?> future = runningTimeouts.get(to);
                if (future != null) {
                    future.cancel(true);
                    runningTimeouts.remove(to);
                    extraDelays.remove(to);
                }
            }
        }
    }

    @Override
    public void startTimeout(SpecifiedTimeout which) {
        startTimeout(which, getDefaultTimeoutSeconds(which));
    }

    @Override
    public void startTimeout(SpecifiedTimeout which, int fixedDelay) {
        startTimeout(which, fixedDelay, TimeUnit.SECONDS);
    }

    @Override
    public void startTimeout(SpecifiedTimeout which, int amount, TimeUnit unit) {
        if (amount < 0) {
            throw new IllegalArgumentException("Timeout delay cannot be negative!");
        }
        synchronized (this) {
            ScheduledFuture<?> existing = runningTimeouts.get(which);
            if (existing != null) {
                throw new IllegalStateException("cannot start timeout "
                    + which
                    + "while it is already running");
            }
            runningTimeouts.put(
                which, timeoutExecutor.schedule(
                    TIMED_OUT.get(which),
                    amount,
                    unit
                )
            );
        }
    }

    private void timedOut(SpecifiedTimeout which) {
        synchronized (this) {
            runningTimeouts.remove(which);
            Integer extra = extraDelays.remove(which);
            if (extra != null) {
                runningTimeouts.put(
                    which, timeoutExecutor.schedule(
                        TIMED_OUT.get(which),
                        extra,
                        TimeUnit.SECONDS
                    )
                );
            }
            else {
                actions.onTimeoutExpired(which, this);
            }
        }
    }

    @Override
    public Integer getTimeoutStatus(SpecifiedTimeout which) {
        synchronized (this) {
            ScheduledFuture<?> future = runningTimeouts.get(which);
            if (future == null) {
                return null;
            }
            Integer extra = extraDelays.getOrDefault(which, 0);
            return Math.toIntExact(future.getDelay(TimeUnit.SECONDS)) + extra;
        }
    }

    @Override
    public boolean isServerSide() {
        return isServer;
    }

    @Override
    public boolean requireTrust(int minimumTrust) {
        return getTrustLevel() >= minimumTrust;
    }

    public int getTrustLevel() {
        return shipConnection.getTrustLevel();
    }

    @Override
    public void increaseTimeout(SpecifiedTimeout which) {
        increaseTimeout(which, getDefaultTimeoutSeconds(which));
    }

    @Override
    public void increaseTimeout(SpecifiedTimeout which, int secondsDelta) {
        synchronized (this) {
            ScheduledFuture<?> future = runningTimeouts.get(which);
            if (future == null) {
                return;
            }
            extraDelays.compute(
                which,
                (_k, old) -> old == null ? secondsDelta : (old + secondsDelta)
            );
        }
    }

    @Override
    public void setExtraData(Object stateExtraData) {
        if (extraDataType == null || !extraDataType.isInstance(stateExtraData)) {
            throw new IllegalArgumentException(
                String.format("Extra data of type %s must match declared type %s for state %s", stateExtraData.getClass(), extraDataType, state));
        }
        this.stateExtraData = stateExtraData;
    }

    @Override
    public <T> T getExtraData(Class<T> dataClass) {
        if (extraDataType == null || !dataClass.isAssignableFrom(extraDataType)) {
            throw new IllegalArgumentException(
                "Extra data must match declared type: " + extraDataType);
        }
        return dataClass.isInstance(stateExtraData)
            ? dataClass.cast(stateExtraData)
            : null;
    }

    @Override
    public UserInterface getUserInterface() {
        return userInterface;
    }

    @Override
    public void enableConnectionDataExchange() {
        shipConnection.enableConnectionDataExchange();
    }

    @Override
    public void setPeerSkiAuthenticated() {
        // TODO get rid of this icky global state
        KeyManagement.setTrustedSkiAuthenticated(shipConnection.getRemoteSki());
    }

    public synchronized void setCommPartnerTrusted() {
        if (state == null || state.ordinal() >= State.CONNECTION_DATA_PREPARATION.ordinal()) {
            // we are not yet in SME Hello, so there is no need to do anything:
            // the state handlers will check for trust when going into SME Hello
            // and see the updated trust level.
            LOGGER.trace("received setCommPartnerTrusted() before SME Hello, trust will be checked"
                + " again when entering SME Hello");
        } else switch (state) {
            case SME_HELLO_STATE_PENDING_LISTEN:
                transitionTo(State.SME_HELLO_STATE_READY_LISTEN);
                break;
            case SME_HELLO_STATE_PENDING_TIMEOUT:
            case SME_HELLO_STATE_PENDING_INIT:
                LOGGER.error("received setCommPartnerTrusted() while in transient state! this should not be possible...");
                break;
            default:
                LOGGER.warn("received setCommPartnerTrusted() while not in PENDING state, this doesn't do anything");
        }
    }
}
