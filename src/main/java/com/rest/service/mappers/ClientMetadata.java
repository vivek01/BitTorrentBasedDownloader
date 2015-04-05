package com.rest.service.mappers;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.p2p.peercds.client.Client;

public class ClientMetadata {

	private String torrentName;
	private String torrentDirectory;
	private boolean paused;
	private String infoHash;
	private Client client;
	private boolean error;
	
	public boolean isError() {
		return error;
	}
	public void setError(boolean error) {
		this.error = error;
	}
	public boolean isPaused() {
		return paused;
	}
	public void setPaused(boolean paused) {
		this.paused = paused;
	}
	public String getTorrentName() {
		return torrentName;
	}
	public void setTorrentName(String torrentName) {
		this.torrentName = torrentName;
	}
	public String getTorrentDirectory() {
		return torrentDirectory;
	}
	public void setTorrentDirectory(String torrentDirectory) {
		this.torrentDirectory = torrentDirectory;
	}
	
	@JsonIgnore
	public Client getClient() {
		return client;
	}
	@JsonIgnore
	public void setClient(Client client) {
		this.client = client;
	}
	public String getInfoHash() {
		return infoHash;
	}
	public void setInfoHash(String infoHash) {
		this.infoHash = infoHash;
	}
}
