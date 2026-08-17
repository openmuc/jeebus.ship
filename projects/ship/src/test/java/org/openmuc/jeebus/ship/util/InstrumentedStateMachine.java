/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.util;

import org.jmock.lib.concurrent.DeterministicScheduler;
import org.openmuc.jeebus.ship.node.ShipNodeParameters;
import org.openmuc.jeebus.ship.shipconnection.ShipConnection;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateHandler;
import org.openmuc.jeebus.ship.state.machine.StateHandlerContext;
import org.openmuc.jeebus.ship.state.machine.StateMachine;
import org.openmuc.jeebus.ship.view.UserInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link StateMachine} that keeps track of its history and can be manipulated,
 * suitable for testing.
 */
public class InstrumentedStateMachine extends StateMachine {
    private final List<byte[]> messagesSent = new ArrayList<>();
    private boolean connectionClosed = false;
    private boolean cdeEnabled = false;
    private int trustLevel = 0;
    private final List<State> stateHistory = new ArrayList<>();
    private Set<State> stoppingSet = Set.of();
    private boolean peerSkiAuthenticated = false;

    public InstrumentedStateMachine(boolean isServer, ShipNodeParameters config, State initial) {
        super(new FakeShipConnection(isServer), new DelegatingUserInterface(new ThrowingUserInterface<>()), config);
        if (initial != null) become(initial);
        setTimeoutExecutor(new DeterministicScheduler());
    }

    public InstrumentedStateMachine(boolean isServer, State initial) {
        this(isServer, new ShipNodeParameters(), initial);
    }

    public InstrumentedStateMachine(boolean isServer) {
        this(isServer, null);
    }

    @Override
    public DelegatingUserInterface getUserInterface() {
        return ((DelegatingUserInterface) super.getUserInterface());
    }

    @Override
    public synchronized void transitionTo(State newState) {
        if (state != null && stoppingSet.contains(state)) return;
        super.transitionTo(newState);
    }

    @Override
    protected State swapState(State newState) {
        stateHistory.add(newState);
        return super.swapState(newState);
    }

    public List<State> getStateHistory() {
        return stateHistory;
    }

    @Override
    public void sendMessage(byte[] msg) {
        Objects.requireNonNull(msg);
        messagesSent.add(msg);
    }

    public List<byte[]> getMessagesSent() {
        return messagesSent;
    }

    public byte[] getLatestMessage() {
        return messagesSent.isEmpty() ? null : messagesSent.get(messagesSent.size()-1);
    }

    @Override
    public void closeConnection() {
        connectionClosed = true;
    }

    public boolean isConnectionClosed() {
        return connectionClosed;
    }

    @Override
    public int getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(int trustLevel) {
        this.trustLevel = trustLevel;
    }

    public void setUserInterface(UserInterface userInterface) {
        getUserInterface().setDelegate(userInterface);
    }

    @Override
    public void enableConnectionDataExchange() {
        this.cdeEnabled = true;
    }

    public boolean isCdeEnabled() {
        return cdeEnabled;
    }

    /**
     * Skip directly to the given state, dropping all data and context associated
     * with the previous state, and ignoring allowed transitions.
     * <p>
     * Does not run the new state's {@link StateHandler#onEntered(State, StateHandlerContext)}.
     * @param newState the state to switch to
     */
    public void become(State newState) {
        stateHistory.clear();
        swapState(newState);
    }

    /**
     * "Manually" run the current state's {@link StateHandler#onEntered(State, StateHandlerContext)}.
     * @param prevState the previous state to present to the handler
     */
    public void triggerEntered(State prevState) {
        actions.onEntered(prevState, this);
    }

    /**
     * Configure a set of states which the machine is not allowed to exit.
     * This is useful for writing smaller, more isolated tests.
     * @param stoppingSet the set of stopping states.
     */
    public void stoppingSet(Set<State> stoppingSet) {
        this.stoppingSet = stoppingSet;
    }

    public DeterministicScheduler getTimeoutExecutor() {
        return (DeterministicScheduler) timeoutExecutor;
    }

    public ShipConnection getShipConnection() {
        return shipConnection;
    }

    @Override
    public void setPeerSkiAuthenticated() {
        peerSkiAuthenticated = true;
    }

    public boolean isPeerSkiAuthenticated() {
        return peerSkiAuthenticated;
    }

    public void setPeerSkiAuthenticated(boolean peerSkiAuthenticated) {
        this.peerSkiAuthenticated = peerSkiAuthenticated;
    }
}
