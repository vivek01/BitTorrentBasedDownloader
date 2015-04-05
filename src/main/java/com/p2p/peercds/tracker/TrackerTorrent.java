package com.p2p.peercds.tracker;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;

import com.p2p.peercds.common.Peer;
import com.p2p.peercds.common.protocol.TrackerMessage.AnnounceRequestMessage.RequestEvent;

interface TrackerTorrent {
	
	 List<Peer> getSomePeers(TrackedPeer peer);
	 
	 TrackedPeer update(RequestEvent event, ByteBuffer peerId,
				String hexPeerId, String ip, int port, long uploaded, long downloaded,
				long left) throws UnsupportedEncodingException;
	 
	 TrackedPeer getPeer(String peerId);
	 
	 int getAnnounceInterval();
	 
	 int seeders();
	 
	 int leechers();
	 
	 void collectUnfreshPeers();
	 
	 String getName();
	 
	 String getHexInfoHash();
	 
	 void addPeer(TrackedPeer peer);
	 
	 TrackedPeer removePeer(String hexPeerId);
}
