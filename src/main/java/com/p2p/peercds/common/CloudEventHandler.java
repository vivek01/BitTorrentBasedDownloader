package com.p2p.peercds.common;

import static com.p2p.peercds.common.Constants.BUCKET_NAME;
import static com.p2p.peercds.common.Constants.CLOUD_PIECE_FETCH_RATIO;
import static com.p2p.peercds.common.Constants.KEY_BUCKET_FORMAT;
import static com.p2p.peercds.common.Constants.PIECE_LENGTH;
import static com.p2p.peercds.common.Constants.DIRECTORY_RANGE_FETCH_KEY_FORMAT;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.p2p.peercds.client.Piece;
import com.p2p.peercds.client.SharedTorrent;
import com.p2p.peercds.client.peer.PeerActivityListener;
import com.p2p.peercds.client.peer.SharingPeer;
import com.p2p.peercds.client.storage.FileCollectionStorage;
import com.p2p.peercds.client.storage.FileCollectionStorage.FileOffset;

public class CloudEventHandler implements SubscriberExceptionHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(CloudEventHandler.class);

	private Lock lock;
	private Set<PeerActivityListener> peerActivityListners;

	public CloudEventHandler() {
		logger.info("Empty constructor called");
	}

	public CloudEventHandler(Lock lock) {
		super();
		this.lock = lock;
		this.peerActivityListners = new HashSet<PeerActivityListener>();
	}

	@Subscribe
	public void fetchMissingPiecesFromCloud(CloudFetchEvent event) {
		logger.info("Handling cloud piece fetch event");
		lock.lock();
		ConcurrentMap<String, SharingPeer> connected = event.getConnected();
		SharedTorrent torrent = event.getTorrent();
		BitSet requestedPieces = torrent.getRequestedPieces();
		BitSet availablePieces = null;
		List<Integer> selectedPieceIndexList = new ArrayList<Integer>();

		synchronized (requestedPieces) {
			if (connected.size() == 0) {
				logger.info("There are no peers connected for the download of this torrent.Getting the piece availability from the pieces completed till now ");
				availablePieces = torrent.getCompletedPieces();
			} else {
				logger.info(connected.size()
						+ " peers are connected for this download. Calculating piece availability based upon the piece availability stats from each peer");
				availablePieces = new BitSet(torrent.getPieceCount());
				for (SharingPeer peer : connected.values()) {
					availablePieces.or(peer.getAvailablePieces());
				}
			}
			synchronized (availablePieces) {

				if (availablePieces.cardinality() == torrent.getPieceCount()) {
					logger.info("All the pieces are available, torrent download is complete. No action required.");
					return;
				}

				int unavailablePieceCount = torrent.getPieceCount()
						- availablePieces.cardinality();
				logger.info(unavailablePieceCount
						+ " pieces are unavailable. 10% of unavailble pieces are fetched for every cluod fetch event.");

				int numPiecesToBeFetched = (int) Math
						.floor(unavailablePieceCount * CLOUD_PIECE_FETCH_RATIO);

				if (numPiecesToBeFetched == 0)
					numPiecesToBeFetched++;

				logger.info(numPiecesToBeFetched
						+ " pieces will be fetched out of "
						+ unavailablePieceCount
						+ " unavailable piece in this cloud fetch event");
				int numPiecesSelected = 0;

				for (int i = availablePieces.nextClearBit(0); i >= 0
						&& numPiecesSelected < numPiecesToBeFetched; i = availablePieces
						.nextClearBit(i + 1)) {
					logger.info("adding piece: " + i
							+ " to the selected piece list");
					selectedPieceIndexList.add(i);
					numPiecesSelected++;
				}
				for (Integer index : selectedPieceIndexList) {
					logger.info("Setting piece " + index
							+ " as requested from cloud");
					torrent.setPieceRequested(index);
				}
			}
		}

		if (!torrent.isMultifile()) {
			logger.info("Handling cloud fetch event for single file torrent");
			for (Integer index : selectedPieceIndexList) {
				Piece piece = torrent.getPiece(index);
				logger.info("Attempting to fetch the piece: " + piece
						+ " from cloud");
				try {
					byte[] data = CloudHelper.downloadPiece(BUCKET_NAME,
							torrent.getCloudKey(), piece.getOffset(),
							piece.getOffset() + piece.size(), false);
					piece.record(ByteBuffer.wrap(data), 0);
					boolean valid = piece.validate();
					if (!valid) {
						logger.warn("Piece: "
								+ piece
								+ " fetched from cloud is invalid. Marking piece as non-requested");
						requestedPieces.clear(index);
					} else {
						logger.info("Piece: "
								+ piece
								+ " has been successfully downloaded from cloud. Firing piece completed event");
						this.firePieceCompleted(piece);
					}
				} catch (TruncatedPieceReadException e) {
					logger.error("Could not read complete piece " + piece
							+ " from cloud. Marking the piece as non-requested",
							e);
					requestedPieces.clear(index);
				} catch (S3ObjectNotFoundException e) {
					logger.error(
							"No object found for key: "
									+ String.format(KEY_BUCKET_FORMAT,
											BUCKET_NAME, torrent.getCloudKey())
									+ " in cloud. Marking the piece as non-requested",
							e);
					requestedPieces.clear(index);
				} catch (FetchSizeExceededException e) {
					logger.error(
							"Fetch size exceeded for key: "
									+ String.format(KEY_BUCKET_FORMAT,
											BUCKET_NAME, torrent.getCloudKey())
									+ " Requested: " + piece.size()
									+ " max allowed is: " + PIECE_LENGTH
									+ ". Marking the piece as non-requested", e);
					requestedPieces.clear(index);
				} catch (S3FetchException e) {
					logger.error(
							"Exception while fetching the data for key: "
									+ String.format(KEY_BUCKET_FORMAT,
											BUCKET_NAME, torrent.getCloudKey())
									+ ". Marking the piece as non-requested", e);
					requestedPieces.clear(index);
				} catch (IOException e) {
					logger.error("IOException while writing piece " + piece
							+ " to the file. Marking piece as non-requested", e);
				}
			}
		} else {
			logger.info("Handling cloud fetch event for multi-file torrent");
			FileCollectionStorage bucket = (FileCollectionStorage) torrent
					.getBucket();
outer:			for (Integer index : selectedPieceIndexList) {
				Piece piece = torrent.getPiece(index);
				ByteBuffer holder = ByteBuffer.allocate((int) piece.size());
				List<FileOffset> fileList = bucket.select(piece.getOffset(),
						piece.size());
				for (FileOffset fileOffset : fileList) {
					logger.info("Piece-" + piece + " corresponds to file: "
							+ fileOffset.getFile().getFileName()
							+ " from offset: " + fileOffset.getOffset()
							+ " with length: " + fileOffset.getLength());
					logger.info("Fetching the piece/block from cloud with key: "
							+ String.format(DIRECTORY_RANGE_FETCH_KEY_FORMAT,
									torrent.getCloudKey(),
									fileOffset.getFileName())
							+ " from offset: "
							+ fileOffset.getOffset()
							+ " with length:" + fileOffset.getLength());
					try {
						String fileName = fileOffset.getFileName();
						File parent = fileOffset.getFile().getTarget().getParentFile();
						while(!parent.getName().equalsIgnoreCase(torrent.getName())){
							fileName = parent.getName()+File.separator+fileName;
							parent = parent.getParentFile();
							}
						logger.info("Multi file cloud key: "+fileName);
						byte[] pieceData = CloudHelper
								.downloadByteRangeFromDirectory(BUCKET_NAME,
										torrent.getCloudKey(),
										fileName,
										(int) fileOffset.getOffset(),
										(int) fileOffset.getLength());
						holder.put(pieceData);
					} catch (S3ObjectNotFoundException e) {
						logger.error(
								"No object found for key: "
										+ String.format(KEY_BUCKET_FORMAT,
												BUCKET_NAME, String.format(
														KEY_BUCKET_FORMAT,
														torrent.getCloudKey(),
														fileOffset
																.getFileName()))
										+ " in cloud. Marking the piece as non-requested",
								e);
						requestedPieces.clear(index);
						continue outer;
					} catch (FetchSizeExceededException e) {
						logger.error(
								"Fetch size exceeded for key: "
										+ String.format(KEY_BUCKET_FORMAT,
												BUCKET_NAME, String.format(
														KEY_BUCKET_FORMAT,
														torrent.getCloudKey(),
														fileOffset
																.getFileName()))
										+ " Requested: "
										+ piece.size()
										+ " max allowed is: "
										+ PIECE_LENGTH
										+ ". Marking the piece as non-requested",
								e);
						requestedPieces.clear(index);
						continue outer;
					} catch (S3FetchException e) {
						logger.error(
								"Exception while fetching the data for key: "
										+ String.format(KEY_BUCKET_FORMAT,
												BUCKET_NAME, String.format(
														KEY_BUCKET_FORMAT,
														torrent.getCloudKey(),
														fileOffset
																.getFileName()))
										+ ". Marking the piece as non-requested",
								e);
						requestedPieces.clear(index);
						continue outer;
					} catch (TruncatedPieceReadException e) {
						logger.error(
								"Could not read complete piece "
										+ piece
										+ "from cloud. Marking the piece as non-requested",
								e);
						requestedPieces.clear(index);
						continue outer;

					}
				}
				if(holder.limit() != piece.size()){
					logger.error(
							"Incomplete Data read from  cloud for"
									+ piece
									+ "Marking the piece as non-requested");			
					requestedPieces.clear(index);
					continue outer;
				}
				try {
					holder.position(0);
					piece.record(holder, 0);
				} catch (IOException e) {
					logger.error("IOException while writing piece: "+piece+" to the underlying file/s");
					continue outer;
				}
				try{
				boolean valid = piece.validate();
				if (!valid) {
					logger.warn("Piece: "
							+ piece
							+ " fetched from cloud is invalid. Marking piece as non-requested");
					requestedPieces.clear(index);
				}else{
					logger.info("Piece: "
							+ piece
							+ " has been successfully downloaded from cloud. Firing piece completed event");
					this.firePieceCompleted(piece);
				}
				}catch(IOException ioe){
					logger.error("IOException while validating the  piece: "+piece);
					continue outer;
				}
			}
		}
		lock.unlock();
		logger.info("Cloud piece fetch event handling complete");
	}

	private void firePieceCompleted(Piece piece) {
		for (PeerActivityListener listener : this.peerActivityListners) {
			try {
				listener.handlePieceCompleted(null, piece);
			} catch (IOException e) {
				logger.error("IOException while firing piece completed event",
						e);
			}
		}
	}

	public void registerPeerActivityListener(PeerActivityListener listener) {
		peerActivityListners.add(listener);
	}

	@Override
	public void handleException(Throwable exception,
			SubscriberExceptionContext context) {
		logger.error("Error handling the cloud fetch event: Reason: "
				+ exception.getMessage(), exception);
	}
}
