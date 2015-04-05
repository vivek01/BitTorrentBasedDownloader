package com.p2p.peercds.tracker;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.p2p.peercds.common.Peer;
import com.p2p.peercds.common.protocol.TrackerMessage.AnnounceRequestMessage.RequestEvent;

class UntrackedTorrent implements TrackerTorrent {
	
	private static final Logger logger =
			LoggerFactory.getLogger(UntrackedTorrent.class);

		/** Minimum announce interval requested from peers, in seconds. */
		public static final int MIN_ANNOUNCE_INTERVAL_SECONDS = 5;

		/** Default number of peers included in a tracker response. */
		private static final int DEFAULT_ANSWER_NUM_PEERS = 30;

		/** Default announce interval requested from peers, in seconds. */
		private static final int DEFAULT_ANNOUNCE_INTERVAL_SECONDS = 10;

		private int answerPeers;
		private int announceInterval;

		/** Peers currently exchanging on this torrent. */
		private ConcurrentMap<String, TrackedPeer> peers;
		private String name;
		private String hexInfoHash;

		/**
		 * Create a new un-tracked torrent
		 *
		 * @param torrent The meta-info byte data.
		 * @throws IOException When the info dictionary can't be
		 * encoded and hashed back to create the torrent's SHA-1 hash.
		 */
		public UntrackedTorrent(String name , String hexInfoHash) throws IOException {
			
			this.name = name;
			this.hexInfoHash = hexInfoHash;
			this.peers = new ConcurrentHashMap<String, TrackedPeer>();
			this.answerPeers = UntrackedTorrent.DEFAULT_ANSWER_NUM_PEERS;
			this.announceInterval = UntrackedTorrent.DEFAULT_ANNOUNCE_INTERVAL_SECONDS;
		}

		
		/**
		 * Returns the map of all peers currently exchanging on this torrent.
		 */
		public Map<String, TrackedPeer> getPeers() {
			return this.peers;
		}

		/**
		 * Add a peer exchanging on this torrent.
		 *
		 * @param peer The new Peer involved with this torrent.
		 */
		@Override
		public void addPeer(TrackedPeer peer) {
			this.peers.put(peer.getHexPeerId(), peer);
		}

		/**
		 * Retrieve a peer exchanging on this torrent.
		 *
		 * @param peerId The hexadecimal representation of the peer's ID.
		 */
		@Override
		public TrackedPeer getPeer(String peerId) {
			return this.peers.get(peerId);
		}

		/**
		 * Remove a peer from this torrent's swarm.
		 *
		 * @param peerId The hexadecimal representation of the peer's ID.
		 */
		@Override
		public TrackedPeer removePeer(String peerId) {
			return this.peers.remove(peerId);
		}

		/**
		 * Count the number of seeders (peers in the COMPLETED state) on this
		 * torrent.
		 */
		@Override
		public int seeders() {
			int count = 0;
			for (TrackedPeer peer : this.peers.values()) {
				if (peer.isCompleted()) {
					count++;
				}
			}
			return count;
		}

		/**
		 * Count the number of leechers (non-COMPLETED peers) on this torrent.
		 */
		@Override
		public int leechers() {
			int count = 0;
			for (TrackedPeer peer : this.peers.values()) {
				if (!peer.isCompleted()) {
					count++;
				}
			}
			return count;
		}

		/**
		 * Remove unfresh peers from this torrent.
		 *
		 * <p>
		 * Collect and remove all non-fresh peers from this torrent. This is
		 * usually called by the periodic peer collector of the BitTorrent tracker.
		 * </p>
		 */
		@Override
		public void collectUnfreshPeers() {
			for (TrackedPeer peer : this.peers.values()) {
				if (!peer.isFresh()) {
					this.peers.remove(peer.getHexPeerId());
				}
			}
		}

		/**
		 * Get the announce interval for this torrent.
		 */
		@Override
		public int getAnnounceInterval() {
			return this.announceInterval;
		}

		/**
		 * Set the announce interval for this torrent.
		 *
		 * @param interval New announce interval, in seconds.
		 */
		public void setAnnounceInterval(int interval) {
			if (interval <= 0) {
				throw new IllegalArgumentException("Invalid announce interval");
			}

			this.announceInterval = interval;
		}

		/**
		 * Update this torrent's swarm from an announce event.
		 *
		 * <p>
		 * This will automatically create a new peer on a 'started' announce event,
		 * and remove the peer on a 'stopped' announce event.
		 * </p>
		 *
		 * @param event The reported event. If <em>null</em>, means a regular
		 * interval announce event, as defined in the BitTorrent specification.
		 * @param peerId The byte-encoded peer ID.
		 * @param hexPeerId The hexadecimal representation of the peer's ID.
		 * @param ip The peer's IP address.
		 * @param port The peer's inbound port.
		 * @param uploaded The peer's reported uploaded byte count.
		 * @param downloaded The peer's reported downloaded byte count.
		 * @param left The peer's reported left to download byte count.
		 * @return The peer that sent us the announce request.
		 */
		@Override
		public TrackedPeer update(RequestEvent event, ByteBuffer peerId,
				String hexPeerId, String ip, int port, long uploaded, long downloaded,
				long left) throws UnsupportedEncodingException {
				
				TrackedPeer peer = null;
				
				try{
					peer = TrackerHelper.processAnnounceEvent(this, event, peerId, hexPeerId, ip, port, uploaded, downloaded, left);
				}catch(Exception e){
					logger.error("Exception while processing the announce event from client: "+ip, e);
				}
			
				return peer;
			}
		/**
		 * Get a list of peers we can return in an announce response for this
		 * torrent.
		 *
		 * @param peer The peer making the request, so we can exclude it from the
		 * list of returned peers.
		 * @return A list of peers we can include in an announce response.
		 */
		@Override
		public List<Peer> getSomePeers(TrackedPeer peer) {
			List<Peer> peers = new LinkedList<Peer>();
			try{
				peers = TrackerHelper.selectPeers(this.peers, peer, this.answerPeers);
			}catch(Exception e){
				logger.error("Exception while selecting peers for announce response", e);
			}

			return peers;
		}


		@Override
		public String getName() {
			
			return name;
		}


		@Override
		public String getHexInfoHash() {
			
			return hexInfoHash;
		}
}
