package org.openmuc.jeebus.ship.api;

import java.time.Instant;

public class ShipConnectionInfoSnapshot {
	private final ConnectionTypeEnum connectionType;
	private final String ski;
	private final String remoteIP;
	private final Integer trustLevel;
	private final boolean isDataExchangeEstablished;
	private final Instant connectionDate;

	public ShipConnectionInfoSnapshot(ConnectionTypeEnum connectionType, String ski, String remoteIP,
									  Integer trustLevel, boolean isDataExchangeEstablished,
									  Instant connectionDate) {
		this.connectionType = connectionType;
		this.ski = ski;
		this.remoteIP = remoteIP;
		this.trustLevel = trustLevel;
		this.isDataExchangeEstablished = isDataExchangeEstablished;
		this.connectionDate = connectionDate;
	}

	public ConnectionTypeEnum getConnectionType() {
		return this.connectionType;
	}

	public String getSki() {
		return this.ski;
	}

	public String getRemoteIP() {
		return this.remoteIP;
	}

	public Integer getTrustLevel() {
		return this.trustLevel;
	}

	public boolean isDataExchangeEstablished() {
		return this.isDataExchangeEstablished;
	}

	public Instant getConnectionDate() {
		return this.connectionDate;
	}

	public static enum ConnectionTypeEnum {
		CLIENT_CONNECTION_TO_PEER, PEER_CONNECTED_TO_SERVER
	}

}
