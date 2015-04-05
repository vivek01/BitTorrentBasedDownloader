package com.p2p.peercds.common;

import java.util.concurrent.ConcurrentMap;

import com.p2p.peercds.client.SharedTorrent;
import com.p2p.peercds.client.peer.SharingPeer;

public class CloudFetchEvent {
	
	private ConcurrentMap<String, SharingPeer> connected;
	private SharedTorrent torrent;

	public CloudFetchEvent(ConcurrentMap<String, SharingPeer> connected , SharedTorrent torrent) {
		super();
		this.connected = connected;
		this.torrent = torrent;
	}

	public ConcurrentMap<String, SharingPeer> getConnected() {
		return connected;
	}

	public SharedTorrent getTorrent() {
		return torrent;
	}
}
