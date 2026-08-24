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

import org.openmuc.jeebus.ship.api.DisconnectReason;
import org.openmuc.jeebus.ship.api.ShipConnectionInterface;
import org.openmuc.jeebus.ship.message.MessageType;
import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.cde.CDEMsg;
import org.openmuc.jeebus.ship.message.connectionclose.CloseMsg;
import org.openmuc.jeebus.ship.message.connectionclose.ConnectionCloseReasonType;
import org.openmuc.jeebus.ship.node.ShipNodeParameters;
import org.openmuc.jeebus.ship.node.KeyManagement;
import org.openmuc.jeebus.ship.node.ShipNodeContext;
import org.openmuc.jeebus.ship.node.websocket.SkiManagementInfo;
import org.openmuc.jeebus.ship.node.websocket.WebSocketHandler;
import org.openmuc.jeebus.ship.state.AccessMethodsIdentification;
import org.openmuc.jeebus.ship.state.ConnectionDataExchange;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateMachine;
import org.openmuc.jeebus.ship.view.CommandLineInput;
import org.openmuc.jeebus.ship.view.UserInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.CompletableFuture;

import static org.openmuc.jeebus.ship.message.connectionclose.ConnectionClosePhaseType.CONFIRM;
import static org.openmuc.jeebus.ship.node.ShipNodeParameters.MINIMAL_TRUST_LEVEL;

// TODO replace usages of ShipConnectionImpl with ShipConnection (or other interface)
public class ShipConnectionImpl implements ShipConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShipConnectionImpl.class);

    public enum Role {
        CLIENT,
        SERVER
    }

    private final Role role;

    private final StateMachine stateMachine;

    private final ShipNodeContext nodeContext;
    /**
     * possibly-forced trust level, overriding the trust level configured for the
     * peer's SKI.
     * <ul>
     *  <li>if -1, trust level is not overridden</li>
     *  <li>if any other value, that is the trust level to be used</li>
     * </ul>
     */
    private int forcedTrustLevel;

    private final WebSocketHandler webSocketHandler;

    private final String peerSki;

    // TODO: definately move to ShipNode
    private UserInterface userInterface;

    // when not null, Connection Data Exchange is enabled
    private ConnectionDataExchange cde;

    // Future to be completed when connection is established
    private final CompletableFuture<ShipConnectionInterface> connectionFuture;
    /**
     * stores outgoing CDE messages while CDE is not yet enabled.
     * they are sent immediately when CDE is enabled.
     */
    private final Queue<CDEMsg> outgoingCdeQueue = new ConcurrentLinkedQueue<>();
    /**
     * stores incoming CDE messages while AMI is not yet through.
     * they are processed immediately when it is.
     */
    private final Queue<byte[]> incomingCdeQueue = new ConcurrentLinkedQueue<>();

    private AccessMethodsIdentification ami;
    // stores an AccessMethodsRequest in case Connection Data Exchange is not enabled yet
    private final Queue<byte[]> queuedAmiMessages = new ConcurrentLinkedQueue<>();

    private final CloseHandler closeHandler = new CloseHandler(this);

    public ShipConnectionImpl(
        Role role,
        int trustLevel,
        ShipNodeContext nodeContext,
        WebSocketHandler webSocketHandler
    ) {
        this.role = role;
        if (trustLevel > 0) {
            this.forcedTrustLevel = trustLevel;
        } else {
            this.forcedTrustLevel = -1;
        }
        this.nodeContext = nodeContext;

        this.webSocketHandler = webSocketHandler;
        this.peerSki = webSocketHandler.getPeerSki();

        // TODO: make
        this.userInterface = new CommandLineInput();

        this.connectionFuture = new CompletableFuture<>();

        this.stateMachine = new StateMachine(this, userInterface);
    }

    @Override
    public CompletableFuture<ShipConnectionInterface> start() {
        stateMachine.begin();
        return this.connectionFuture;
    }

    public void onMessage(byte[] message) {
        LOGGER.debug(
            "{} received message:\n" + MessageUtility.parseShipMsgToString(message),
            getLogPrefix()
        );

        Optional<MessageType> type = MessageType.fromValue(message[0]);

        if (type.isEmpty()) {
            LOGGER.error(
                "{} received a message with an unrecognized MessageType: {}",
                getLogPrefix(),
                message[0]
            );
        }
        else {
            switch (type.get()) {
                case INIT:
                case CONTROL:
                    if (new String(message, StandardCharsets.UTF_8)
                        .contains("accessMethods")
                    ) {
                        if (cde == null) {
                            LOGGER.warn(
                                "accessMethods message was received but Connection Data"
                                    + " Exchange is not enabled. Queuing it until it is."
                            );
                            queuedAmiMessages.add(message);
                        }
                        else {
                            if (ami == null) {
                                ami = new AccessMethodsIdentification(this);
                            }
                            ami.processMsg(message);
                        }
                    }
                    else if (stateMachine.getState() == State.SME_HELLO_OK) {
                        LOGGER.error("received message in HELLO_OK state");
                    }
                    else {
                        stateMachine.messageReceived(message);
                    }
                    break;
                case DATA:
                    // CDE message
                    if (cde == null || ami == null || ami.getAmMsg() == null ) {
                        LOGGER.debug(
                            "Connection Data Exchange message received prematurely."
                            + " Queuing it for later processing."
                        );
                        incomingCdeQueue.add(message);
                    }
                    else {
                        cde.processMsg(message);
                    }
                    break;
                case END:
                    // Connection Close message
                    if (cde != null) {
                        closeHandler.processMsg(message);
                    } else {
                        LOGGER.error(
                            "Connection Close should not be requested without entering Connection Data Exchange before");
                    }
                    break;
            }
        }
    }

    public boolean isServer() {
        return role == Role.SERVER;
    }

    synchronized State getState() {
        return stateMachine.getState();
    }

    public void stopStateTimeouts() {
        stateMachine.stopAllTimeouts();
    }

    public void initiateConnectionClose(
        Integer maxTime,
        ConnectionCloseReasonType reason
    ) {
        if (cde == null) {
            throw new IllegalStateException(
                "connectionClose should only be initiated after Connection Data Exchange was entered");
        }
        closeHandler.initiate(maxTime, reason);
    }

    @Override
    public int getTrustLevel() {
        if (forcedTrustLevel >= 0) {
            return forcedTrustLevel;
        } else {
            SkiManagementInfo skiManagementInfo = this.nodeContext
                .getKeyManagement()
                .getTrustedSkis()
                .get(webSocketHandler.getPeerSki());
            if (skiManagementInfo == null) {
                return 0;
            } else {
                return skiManagementInfo.getTrustLevel();
            }
        }
    }

    /**
     * Set the trust level for this connection, overriding any trust level configured
     * for the peer's SKI.
     * @param trustLevel the trust level to set.
     * @see #disableForcedTrustLevel()
     */
    public void setTrustLevel(int trustLevel) {
        if (trustLevel < 0 || trustLevel > 96) {
            throw new IllegalArgumentException("trust level should be a positive value at most 96");
        }
        this.forcedTrustLevel = trustLevel;
    }

    /**
     * Stop forcing a trust level, relying instead on the trust level recorded for
     * the peer's SKI in the {@link KeyManagement}.
     * @see #setTrustLevel(int)
     */
    public void disableForcedTrustLevel() {
        forcedTrustLevel = -1;
    }

    public boolean isForceTrusted() {
        return forcedTrustLevel >= MINIMAL_TRUST_LEVEL;
    }

    /**
     * Set the communication partner to be trusted, allowing communication to
     * proceed.
     * <p>
     * This is only allowed if the trust level for this connection is &ge;
     * {@link ShipNodeParameters#MINIMAL_TRUST_LEVEL}.
     */
    public void trustCommPartner() {
        if (getTrustLevel() < MINIMAL_TRUST_LEVEL) {
            LOGGER.error(
                "{}: trust level should be higher than {} to proceed",
                getLogPrefix(),
                MINIMAL_TRUST_LEVEL
            );
            throw new IllegalStateException(
                "trust level should be higher than "+MINIMAL_TRUST_LEVEL+" to proceed");
        }
        stateMachine.setCommPartnerTrusted();
    }

    public ShipConnectionInterface getApiShipConnection() {
        return this;
    }

    public boolean isConnectionCloseState() {
        return closeHandler.isClosing();
    }

    public ShipNodeContext getShipNodeContext() {
        return this.nodeContext;
    }

    public String getLogPrefix() {
        return nodeContext.getLogPrefix();
    }

    public ConnectionDataExchange getCde() {
        return cde;
    }

    public void requestAccessMethods() {
        if (ami == null) {
            throw new IllegalStateException(
                "Connection Data Exchange should be enabled before requesting access methods");
        }

        ami.sendRequest();
    }

    @Override
    public void enableConnectionDataExchange() {
        cde = new ConnectionDataExchange(
            this,
            nodeContext.getConnHandler()
        );
        if (ami == null) {
            ami = new AccessMethodsIdentification(this);
        }
        ami.sendRequest();
        while (!queuedAmiMessages.isEmpty()) {
            ami.processMsg(queuedAmiMessages.poll());
        }

        connectionFuture
            .orTimeout(ShipNodeParameters.AMI_TIMEOUT, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable instanceof TimeoutException) {
                    closeImmediately();
                    LOGGER.warn(
                        "{}: Closing connection: Did not receive a proper accessMethods message containing their SHIP ID within {} seconds.",
                        getLogPrefix(),
                        ShipNodeParameters.AMI_TIMEOUT
                    );
                }
            });
    }

    public void connectionEstablished() {
        nodeContext.setLogPrefix(nodeContext.getLogPrefix().split("to")[0] + "to " + getRemoteId());

        if (!connectionFuture.isDone()) {
            connectionFuture.complete(this);
        }
        if(nodeContext.getConnHandler() != null && this.role == Role.SERVER) {
            nodeContext.getConnHandler().clientConnected(this);
        }

        while (!incomingCdeQueue.isEmpty()) {
            cde.processMsg(incomingCdeQueue.poll());
        }

        while (!outgoingCdeQueue.isEmpty()) {
            cde.sendCDE(outgoingCdeQueue.poll());
        }
    }

    public void sendCdeMsg(byte[] msg) {
        CDEMsg cdeMsg = new CDEMsg("ee1.0", new String(msg, StandardCharsets.UTF_8));
        if (cde == null) {
            LOGGER.info(
                "{}: message will be sent when Connection Data Exchange mode is enabled",
                getLogPrefix()
            );
            // cde is not enabled yet, so queue the message
            outgoingCdeQueue.add(cdeMsg);
        }
        else {
            if (outgoingCdeQueue.isEmpty()) {
                cde.sendCDE(cdeMsg);
            }
            else {
                LOGGER.info(
                    "{}: Connection Data Exchange message will be queued",
                    getLogPrefix()
                );
                outgoingCdeQueue.add(cdeMsg);
            }
        }
    }

    @Override
    public String getRemoteSki() {
        return peerSki;
    }

    @Override
    public String getRemoteId() {
        return ami.getAmMsg().getId();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return webSocketHandler.getRemoteSocketAddress();
    }

    @Override
    public URI getRemoteUri() {
        return URI.create(ami.getAmMsg().getDns().getUri());
    }

    @Override
    public void sendMsg(byte[] msg) {
        sendCdeMsg(msg);
    }

    @Override
    public void sendRawMessage(byte[] message) {
        if (LOGGER.isDebugEnabled()) {
            // avoid msg->string conversion if debug is not enabled
            LOGGER.debug(
                "{} sending message:\n{}",
                getLogPrefix(),
                MessageUtility.parseShipMsgToString(message)
            );
        }
        webSocketHandler.sendMsg(message);
    }

    public CompletableFuture<ShipConnectionInterface> getConnectionFuture() {
        return connectionFuture;
    }

    public void prepareCDEShutdown() {
        // prepare shutdown and close connection if maxTime is not expired
        if (!closeHandler.getDevB().isMaxTimeExpired()) {
            LOGGER.info(
                "{} stopped connection data exchange before maxTime was reached and will "
                    + "close the connection",
                getLogPrefix()
            );
            CloseMsg closeMsg = new CloseMsg(CONFIRM);
            sendRawMessage(ShipMessageFactory.parseConnectionCloseBody(closeMsg));
            org.openmuc.jeebus.ship.api.ConnectionHandler connHandler = nodeContext.getConnHandler();
            if (connHandler != null) {
                connHandler.onDisconnect(
                    DisconnectReason.REGULAR_END,
                    this
                );
            }
            webSocketHandler.close();
            if (!connectionFuture.isDone()) {
                connectionFuture.completeExceptionally(new CancellationException(
                    "Connection was closed."
                ));
            }
        }
    }

    @Override
    public void closeImmediately() {
        webSocketHandler.close();
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(new CancellationException(
                "Connection was closed."
            ));
        }
    }

    @Override
    public void close() {
        if (cde != null) {
            initiateConnectionClose(
                100,
                ConnectionCloseReasonType.UNSPECIFIC
            );
        }
        else {
            closeImmediately();
        }
    }
}
