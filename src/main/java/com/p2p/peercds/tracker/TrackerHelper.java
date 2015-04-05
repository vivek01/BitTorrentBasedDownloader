package com.p2p.peercds.tracker;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.p2p.peercds.common.Peer;
import com.p2p.peercds.common.protocol.TrackerMessage.AnnounceRequestMessage.RequestEvent;

public class TrackerHelper {
	
	private static final Logger logger =
			LoggerFactory.getLogger(TrackerHelper.class);

	public static List<Peer> selectPeers(ConcurrentMap<String, TrackedPeer> peers ,TrackedPeer peer , int maxPeersNeeded) {
		List<Peer> selectedPeers = new LinkedList<Peer>();

		// Extract answerPeers random peers
		List<TrackedPeer> candidates =
			new LinkedList<TrackedPeer>(peers.values());
		Collections.shuffle(candidates);

		int count = 0;
		for (TrackedPeer candidate : candidates) {
			// Collect unfresh peers, and obviously don't serve them as well.
			if (!candidate.isFresh() ||
				(candidate.looksLike(peer) && !candidate.equals(peer))) {
				logger.debug("Collecting stale peer {}...", candidate);
				peers.remove(candidate.getHexPeerId());
				continue;
			}

			// Don't include the requesting peer in the answer.
			if (peer.looksLike(candidate)) {
				continue;
			}

			// Collect unfresh peers, and obviously don't serve them as well.
			if (!candidate.isFresh()) {
				logger.debug("Collecting stale peer {}...",
					candidate.getHexPeerId());
				peers.remove(candidate.getHexPeerId());
				continue;
			}

			// Only serve at most ANSWER_NUM_PEERS peers
			if (count++ >maxPeersNeeded) {
				break;
			}

			selectedPeers.add(candidate);
		}

		return selectedPeers;
	}
	
	public static TrackedPeer processAnnounceEvent(TrackerTorrent torrent ,RequestEvent event, ByteBuffer peerId,
			String hexPeerId, String ip, int port, long uploaded, long downloaded,
			long left) throws UnsupportedEncodingException {
			TrackedPeer peer;
			TrackedPeer.PeerState state = TrackedPeer.PeerState.UNKNOWN;

			if (RequestEvent.STARTED.equals(event)) {
				peer = new TrackedPeer(torrent, ip, port, peerId);
				state = TrackedPeer.PeerState.STARTED;
				torrent.addPeer(peer);
			} else if (RequestEvent.STOPPED.equals(event)) {
				peer = torrent.removePeer(hexPeerId);
				state = TrackedPeer.PeerState.STOPPED;
			} else if (RequestEvent.COMPLETED.equals(event)) {
				peer = torrent.getPeer(hexPeerId);
				state = TrackedPeer.PeerState.COMPLETED;
			} else if (RequestEvent.NONE.equals(event)) {
				peer = torrent.getPeer(hexPeerId);
				state = TrackedPeer.PeerState.STARTED;
			} else {
				throw new IllegalArgumentException("Unexpected announce event type!");
			}

			peer.update(state, uploaded, downloaded, left);
			return peer;
		}
}
