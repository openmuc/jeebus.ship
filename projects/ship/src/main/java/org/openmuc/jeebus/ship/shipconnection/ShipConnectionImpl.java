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

import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.api.DisconnectReason;
import org.openmuc.jeebus.ship.api.ShipConnectionInterface;
import org.openmuc.jeebus.ship.message.MessageUtility;
import org.openmuc.jeebus.ship.message.ShipMessageFactory;
import org.openmuc.jeebus.ship.message.cde.CDEMsg;
import org.openmuc.jeebus.ship.message.connectionclose.CloseMsg;
import org.openmuc.jeebus.ship.message.connectionclose.ConnectionCloseReasonType;
import org.openmuc.jeebus.ship.node.StaticConfiguration;
import org.openmuc.jeebus.ship.node.KeyManagement;
import org.openmuc.jeebus.ship.node.ShipNodeContext;
import org.openmuc.jeebus.ship.node.websocket.AuthenticatedConnection;
import org.openmuc.jeebus.ship.node.websocket.SkiManagementInfo;
import org.openmuc.jeebus.ship.state.AccessMethodsIdentification;
import org.openmuc.jeebus.ship.state.ConnectionDataExchange;
import org.openmuc.jeebus.ship.state.machine.State;
import org.openmuc.jeebus.ship.state.machine.StateMachine;
import org.openmuc.jeebus.ship.view.CommandLineInput;
import org.openmuc.jeebus.ship.view.UserInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.openmuc.jeebus.ship.message.connectionclose.ConnectionClosePhaseType.CONFIRM;

// TODO replace usages of ShipConnectionImpl with ShipConnection (or other interface)
public class ShipConnectionImpl implements ShipConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShipConnectionImpl.class);

    public enum Role {
        CLIENT, SERVER
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

    private AuthenticatedConnection connection;

    private final String peerSki;
    private boolean trustCommPartner;

    // TODO: maybe move to ShipNode
    private UserInterface userInterface;

    private int selectedMajor;
    private int selectedMinor;
    private String selectedFormat;

    // when not null, Connection Data Exchange is enabled
    private ConnectionDataExchange cde;
    // stores outgoing CDE messages while CDE is not yet enabled.
    // they will be sent immediately when CDE becomes enabled.
    private final Queue<CDEMsg> cdeMessageQueue = new ConcurrentLinkedQueue<>();

    private AccessMethodsIdentification ami;
    // stores an AccessMethodsRequest in case Connection Data Exchange is not enabled yet
    private byte[] queuedAmrMessage;

    private final CloseHandler closeHandler = new CloseHandler(this);

    public ShipConnectionImpl(
        boolean server,
        int trustLevel,
        ShipNodeContext nodeContext,
        AuthenticatedConnection connection
    ) {
        this.role = server ? Role.SERVER : Role.CLIENT;
        if (trustLevel > 0) {
            forcedTrustLevel = trustLevel;
        } else {
            forcedTrustLevel = -1;
        }
        // minimum trust level is 8
        this.trustCommPartner = trustLevel >= 8;
        this.nodeContext = nodeContext;

        this.connection = connection;
        this.peerSki = connection.getPeerSki();

        // TODO: eventually change to dynamically choose type of UI
        userInterface = new CommandLineInput();

        stateMachine = new StateMachine(this, userInterface, getConfig());
    }

    @Override
    public void initState() {
        stateMachine.begin();
    }

    public void onMessage(byte[] message) {
        LOGGER.debug("{} received message:\n" + MessageUtility.parseShipMsgToString(
            message), getLogPrefix());
        // message with MessageType value of 2 are processed in Connection Data Exchange
        switch (message[0]) {
            case 0:
            case 1:
                if (new String(message, StandardCharsets.UTF_8).contains(
                    "accessMethods")) {
                    if (cde == null) {
                        LOGGER.warn(
                            "access methods request was received but Connection Data"
                                + " Exchange is not enabled"
                        );
                        queuedAmrMessage = message;
                    }
                    else {
                        if (ami == null) {
                            ami = new AccessMethodsIdentification(
                                this,
                                nodeContext.getOwnShipId()
                            );
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
            case 2:
                // CDE message
                if (cde == null) {
                    LOGGER.warn(
                        "message with MessageType = 2 was received but Connection Data Exchange is not enabled");
                }
                else {
                    cde.processMsg(message);
                }
                break;
            case 3:
                // Connection Close message
                if (cde != null) {
                    closeHandler.processMsg(message);
                } else {
                    LOGGER.error(
                        "Connection Close should not be requested without entering Connection Data Exchange before");
                }
                break;
            default:
                LOGGER.error(
                    "{} received a message with an unrecognized MessageType: {}",
                    getLogPrefix(),
                    message[0]
                );
                break;
        }
    }

    public boolean isServer() {
        return role == Role.SERVER;
    }

    public synchronized State getState() {
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
            SkiManagementInfo skiManagementInfo = KeyManagement
                .getTrustedSkis()
                .get(connection.getPeerSki());
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

    public boolean trustsCommPartner() {
        return trustCommPartner;
    }

    /**
     * Set the communication partner to be trusted, allowing communication to proceed.
     * <p>
     * This is only allowed if the trust level for this connection is &ge;8.
     */
    public void trustCommPartner() {
        // minimum trust level for communication is 8 (auto accepted connections)
        if (getTrustLevel() < 8) {
            LOGGER.error(
                "{}: trust level should be higher than 8 to proceed",
                getLogPrefix()
            );
            throw new IllegalStateException(
                "trust level should be higher than 8 to proceed");
        }
        this.trustCommPartner = true;
        stateMachine.setCommPartnerTrusted();
    }

    /**
     * Set the communication partner to untrusted. This does not affect the trust
     * level configured for this connection or for the partner's SKI.
     */
    public void distrustCommPartner() {
        this.trustCommPartner = false;
    }

    public AuthenticatedConnection getConnection() {
        return connection;
    }

    public void setConnection(AuthenticatedConnection basicListener) {
        this.connection = basicListener;
    }

    public int getSelectedMajor() {
        return selectedMajor;
    }

    public void setSelectedMajor(int selectedMajor) {
        this.selectedMajor = selectedMajor;
    }

    public int getSelectedMinor() {
        return selectedMinor;
    }

    public void setSelectedMinor(int selectedMinor) {
        this.selectedMinor = selectedMinor;
    }

    public void setSelectedVersion(int major, int minor) {
        selectedMajor = major;
        selectedMinor = minor;
    }

    public String getSelectedFormat() {
        return selectedFormat;
    }

    public void setSelectedFormat(String selectedFormat) {
        this.selectedFormat = selectedFormat;
    }

    public ShipConnectionInterface getApiShipConn() {
        return this;
    }

    public void setApiShipConn(ShipConnectionInterface shipConnInterface) {
    }

    public boolean isConnectionCloseState() {
        return closeHandler.isClosing();
    }

    public StaticConfiguration getConfig() {
        return nodeContext.getConfig();
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

        if (!isServer()) {
            throw new IllegalStateException(
                "Only servers should request access methods");
        }

        ami.sendRequest();
    }

    @Override
    public void enableConnectionDataExchange() {
        ConnectionHandler connHandler = nodeContext.getConnHandler();
        cde = new ConnectionDataExchange(
            this,
            connHandler
        );
        CDEMsg queuedMsg;
        while ((queuedMsg = cdeMessageQueue.poll()) != null) {
            cde.sendCDE(queuedMsg);
        }
        ami = new AccessMethodsIdentification(this, nodeContext.getOwnShipId());
        if (queuedAmrMessage != null) {
            ami.processMsg(queuedAmrMessage);
        }
        if (Objects.nonNull(connHandler)) {
            connHandler.connectionDataExchangeEnabled(getRemoteAddress());
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
            cdeMessageQueue.add(cdeMsg);
        }
        else {
            if (cdeMessageQueue.isEmpty()) {
                cde.sendCDE(cdeMsg);
            }
            else {
                LOGGER.info(
                    "{}: Connection Data Exchange message will be queued",
                    getLogPrefix()
                );
                cdeMessageQueue.add(cdeMsg);
            }
        }
    }

    @Override
    public void setUserInterface(UserInterface userInterface) {
        this.userInterface = userInterface;
    }

    @Override
    public String getRemoteSki() {
        return peerSki;
    }

    @Override
    public String getRemoteAddress() {
        return connection.getRemoteAddress();
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
                "{} sending message:\n\t{}",
                getLogPrefix(),
                MessageUtility.parseShipMsgToString(message)
            );
        }
        connection.sendMsg(message);
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
            ConnectionHandler connHandler = nodeContext.getConnHandler();
            if (connHandler != null) {
                connHandler.onDisconnect(
                    DisconnectReason.REGULAR_END,
                    this
                );
            }
            connection.close();
        }
    }

    @Override
    public void closeImmediately() {
        connection.close();
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
