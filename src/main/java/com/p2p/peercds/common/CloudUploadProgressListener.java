package com.p2p.peercds.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;

public class CloudUploadProgressListener implements ProgressListener {

	private long uploadSizeInBytes;
	private long numBytesUploaded;
	private long numPartUploads;
	private long numPartsCompleted;

	private static final Logger logger = LoggerFactory
			.getLogger(CloudUploadProgressListener.class);

	public CloudUploadProgressListener(long uploadSizeInBytes,
			long numPartUploads) {
		super();
		this.uploadSizeInBytes = uploadSizeInBytes;
		this.numPartUploads = numPartUploads;
		this.numPartsCompleted = 0;
		this.uploadSizeInBytes = 0;
	}



	@Override
	public void progressChanged(ProgressEvent progressEvent) {

		 if (progressEvent.getEventType() == ProgressEventType.CLIENT_REQUEST_SUCCESS_EVENT){
			numPartsCompleted++;
			numBytesUploaded = numBytesUploaded
					+ progressEvent.getBytesTransferred();
			logger.info("parts completed: "+numPartsCompleted+" total parts: "+numPartUploads);
			if (numPartsCompleted == numPartUploads) {
				logger.info("Cloud upload event recieved:  type: "
						+ progressEvent.getEventType().name());
				logger.info("Cloud upload event completed. Validating the cloud upload");
				if (uploadSizeInBytes == numBytesUploaded)
					logger.info("Cloud upload event completed successfully. Safe to write cloud-key in the file");
				else
					logger.info("Cloud upload event did not upload the expected amount of data on cloud:  Expected : "
							+ uploadSizeInBytes
							+ " Transferred: "
							+ numBytesUploaded + ". Cloud-key is no longer valid");
			}
		}else if (progressEvent.getEventType() == ProgressEventType.CLIENT_REQUEST_FAILED_EVENT)
			logger.error("Cloud upload failed for this torrent. Cloud key is no longer valid");
	}

}
