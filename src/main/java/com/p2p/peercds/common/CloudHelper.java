package com.p2p.peercds.common;

import static com.p2p.peercds.common.Constants.BUCKET_NAME;
import static com.p2p.peercds.common.Constants.DIRECTORY_RANGE_FETCH_KEY_FORMAT;
import static com.p2p.peercds.common.Constants.PIECE_LENGTH;
import static com.p2p.peercds.common.Constants.hiddenFilesFilter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.ObjectMetadataProvider;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.google.common.base.Strings;

public class CloudHelper {

	private static final Logger logger = LoggerFactory
			.getLogger(CloudHelper.class);

	private static AmazonS3 s3 = null;
	private static TransferManager manager = null;
	

	static {

		s3 = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
		s3.setRegion(Region.getRegion(Regions.US_EAST_1));
		manager = new TransferManager(s3){
					
			@Override
			public MultipleFileUpload uploadDirectory(String bucketName, String virtualDirectoryKeyPrefix, File directory, boolean includeSubdirectories, ObjectMetadataProvider metadataProvider) {
		        if ( directory == null || !directory.exists() || !directory.isDirectory() ) {
		            throw new IllegalArgumentException("Must provide a directory to upload");
		        }

		        List<File> files = new LinkedList<File>();
		        listFiles(directory, files, includeSubdirectories);

		        return uploadFileList(bucketName, virtualDirectoryKeyPrefix, directory, files, metadataProvider);
		    }
			
			public void listFiles(File dir, List<File> results, boolean includeSubDirectories) {
		        File[] found = dir.listFiles(hiddenFilesFilter);
		        if ( found != null ) {
		            for ( File f : found ) {
		                if (f.isDirectory()) {
		                    if (includeSubDirectories) {
		                        listFiles(f, results, includeSubDirectories);
		                    }
		                } else {
		                    results.add(f);
		                }
		            }
		        }
		    }
		};
		if (!s3.doesBucketExist(BUCKET_NAME)) {
			logger.info("Creating bucket " + BUCKET_NAME);
			s3.createBucket(BUCKET_NAME);
			logger.info(BUCKET_NAME + " bucket created");
		}

	}

	public static void main(String args[]) throws Exception {

		logger.info("===========================================");
		logger.info("Getting Started with Amazon S3");
		logger.info("===========================================\n");

		File file = new File("/Users/kaustubh/Desktop/mfile");
		uploadTorrent("peer-cds", "mfile1", file,null);
		// downloadPiece("peer-cds", "mfile1/abc.pdf" , 10 , 11, false);
		downloadCompleteDirectory(BUCKET_NAME, "mfile");
		//downloadByteRangeFromDirectory(BUCKET_NAME, "mfile", "abc.pdf", 0, 117143);

	}

	public static boolean uploadTorrent(String bucketName, String key,
			File sourceFile, ProgressListener listener) throws S3FetchException {

		if (Strings.isNullOrEmpty(bucketName) || Strings.isNullOrEmpty(key)
				|| sourceFile == null || !sourceFile.exists())
			throw new IllegalArgumentException(
					"bucket-nam , file-key and file can not be empty or null");
		else {
			bucketName = bucketName.trim();
			key = key.trim();
		}
		
		if (keyExistsInBucket(bucketName, key, null) == 0) {
			String newKey = key;
			if (sourceFile.isDirectory()) {
				newKey = newKey+File.pathSeparator+sourceFile.getName();
				logger.info("Uploading a directory: " + sourceFile.getName()
						+ " to cloud.");

				MultipleFileUpload uploadDirectory = manager.uploadDirectory(bucketName, key, sourceFile, true, null);
				try {
					if(listener!= null)
					uploadDirectory.addProgressListener(listener);
				} catch (Exception e) {
					logger.error("Exception while uploading a directory: "
							+ sourceFile.getName() + " to the cloud", e);
					throw new S3FetchException("Unable to upload directory "
							+ sourceFile.getName() + "to the cloud: Reason: "
							+ e.getMessage());
				}

				logger.info("Directory: " + sourceFile.getName()
						+ " has been uploaded successfully to the cloud.");
				return true;

			} else {

				return uploadFileToCloud(bucketName, newKey, sourceFile , listener);
			}
		} else {

			logger.info("File already exists in the cloud.. skipping upload.");
			return false;
		}
	}
	
	private static boolean uploadFileToCloud(String bucketName , String key , File sourceFile , ProgressListener listener){
		logger.info("Uploading a single file: " + sourceFile.getName()
				+ " to cloud.");
		try{
		   PutObjectRequest putObjectRequest = new PutObjectRequest(
				bucketName, key, sourceFile);
		   Upload upload = manager.upload(putObjectRequest);
		   if(listener!= null)
			   upload.addProgressListener(listener);
		}catch(Exception e){
			logger.error("Exception while uploading file "+sourceFile.getName()+"to the torrent with key: "+key);
			return false;
		}
		
		return true;
	}

	private static int keyExistsInBucket(String bucketName, String keyPrefix,
			List<S3ObjectSummary> objectSummaries) {

		if (objectSummaries == null || objectSummaries.isEmpty())
			objectSummaries = getKeyMetaInfoFromBucket(bucketName, keyPrefix);
		return objectSummaries.size();
	}

	private static List<S3ObjectSummary> getKeyMetaInfoFromBucket(
			String bucketName, String keyPrefix) {
		ObjectListing listObjects = s3.listObjects(bucketName.trim(),
				keyPrefix.trim());
		List<S3ObjectSummary> objectSummaries = listObjects
				.getObjectSummaries();
		return objectSummaries;
	}

	public static byte[] downloadCompleteDirectory(String bucketName,
			String s3DirectoryPrefix) throws S3ObjectNotFoundException,
			FetchSizeExceededException, S3FetchException,
			TruncatedPieceReadException {

		logger.info("Complete directory download has been requested with the directory prefix: "
				+ s3DirectoryPrefix);
		if (Strings.isNullOrEmpty(bucketName)
				|| Strings.isNullOrEmpty(s3DirectoryPrefix))
			throw new IllegalArgumentException(
					"bucket-nam , directory-prefix and file-name can not be empty or null");
		else {
			bucketName = bucketName.trim();
			s3DirectoryPrefix = s3DirectoryPrefix.trim();
		}
		List<S3ObjectSummary> objList = getKeyMetaInfoFromBucket(bucketName,
				s3DirectoryPrefix);
		int numObjsInBucket = keyExistsInBucket(bucketName, s3DirectoryPrefix,
				objList);
		if (numObjsInBucket == 0)
			throw new S3ObjectNotFoundException("No directory with key: "
					+ s3DirectoryPrefix + " in the bucket: " + bucketName
					+ " can be found");
		else {
			if (numObjsInBucket == 1)
				logger.warn("Single object is associated with the directory prefix: "
						+ s3DirectoryPrefix);
			else
				logger.info(numObjsInBucket
						+ " files will be downloaded from directory: "
						+ s3DirectoryPrefix);

			int directorySize = 0;

			for (S3ObjectSummary obj : objList)
				directorySize = (int) (directorySize + obj.getSize());

			if (directorySize > PIECE_LENGTH * 1024)
				throw new FetchSizeExceededException(
						"Max fetch size exceeded. Consider chunking the download in parts. Max allowed size is: "
								+ Integer.MAX_VALUE);

			logger.info(directorySize
					+ "bytes of data will be downloaded from cloud for directory: "
					+ s3DirectoryPrefix);
			logger.info(Math.ceil(directorySize / 1024)
					+ "kb of data will be downloaded from cloud for directory: "
					+ s3DirectoryPrefix);

			ByteBuffer holder = ByteBuffer.allocate(directorySize);
			File tmp = new File("tmp/");
			MultipleFileDownload ddf = manager.downloadDirectory(bucketName,
					s3DirectoryPrefix, tmp);
			try {
				ddf.waitForCompletion();
			} catch (Exception e) {
				logger.error(
						"Exception while waiting for the directory download to complete for directory: "
								+ s3DirectoryPrefix, e);
				throw new S3FetchException(
						"Exception while waiting for the directory download to complete for directory: "
								+ s3DirectoryPrefix);
			}
			try {
				File actual = new File(tmp, s3DirectoryPrefix);
				for (File file : actual.listFiles()) {
					RandomAccessFile raf = new RandomAccessFile(file, "r");
					FileChannel channel = raf.getChannel();
					channel.read(holder);
					channel.close();
				}
				FileUtils.deleteDirectory(actual);
			} catch (Exception e) {
				logger.error(
						"Exception while reading the downloaded directory  with key "
								+ s3DirectoryPrefix, e);
				throw new S3FetchException(
						"Exception while waiting for the directory download to complete for directory: "
								+ s3DirectoryPrefix);
			}
			if (holder.limit() != directorySize)
				throw new TruncatedPieceReadException(
						"Number of bytes expected to be read: " + directorySize
								+ ". Number of bytes actually read: "
								+ holder.limit());
			logger.info("Directory: "
					+ s3DirectoryPrefix
					+ " has been downloaded from the cloud. Returning the byte contents in array");
			return holder.array();
		}
	}

	public static byte[] downloadByteRangeFromDirectory(String bucketName,
			String s3DirectoryPrefix, String fileName, Integer offset,
			Integer length) throws S3ObjectNotFoundException,
			FetchSizeExceededException, S3FetchException,
			TruncatedPieceReadException {

		if (offset == null || length == null)
			throw new IllegalArgumentException(
					"Offset and length must be specified to download the byte range from cloud");

		if (offset < 0 || length < 0)
			throw new IllegalArgumentException(
					"Offset and length can not be less than zero");

		if (length > PIECE_LENGTH * 1024)
			throw new FetchSizeExceededException(
					"Max fetch size exceeded. Consider chunking the download in parts. Max allowed size is piece size: "
							+ PIECE_LENGTH * 1024+"bytes, "+PIECE_LENGTH+" kb");

		if (Strings.isNullOrEmpty(bucketName)
				|| Strings.isNullOrEmpty(s3DirectoryPrefix)
				|| Strings.isNullOrEmpty(fileName))
			throw new IllegalArgumentException(
					"bucket-nam , directory-prefix and file-name can not be empty or null");
		else {
			bucketName = bucketName.trim();
			s3DirectoryPrefix = s3DirectoryPrefix.trim();
			fileName = fileName.trim();
		}

		List<S3ObjectSummary> keyMetaInfoFromBucket = getKeyMetaInfoFromBucket(
				bucketName, s3DirectoryPrefix);

		if (keyExistsInBucket(bucketName, s3DirectoryPrefix,
				keyMetaInfoFromBucket) == 0)
			throw new S3ObjectNotFoundException("Directory: "
					+ s3DirectoryPrefix + " does not exists in the cloud");

		keyMetaInfoFromBucket = getKeyMetaInfoFromBucket(bucketName,
				String.format(DIRECTORY_RANGE_FETCH_KEY_FORMAT, s3DirectoryPrefix,
						fileName));

		if (keyExistsInBucket(bucketName, String.format(
				DIRECTORY_RANGE_FETCH_KEY_FORMAT, bucketName, fileName), keyMetaInfoFromBucket) == 0)
			throw new S3ObjectNotFoundException("Directory: "
					+ s3DirectoryPrefix + " does not contain file: " + fileName
					+ "in the cloud");

		if (keyMetaInfoFromBucket.size() > 1)
			throw new S3ObjectNotFoundException("Directory: "
					+ s3DirectoryPrefix + " has more than 1 file matching : "
					+ fileName + "in the cloud");

		S3ObjectSummary s3ObjectSummary = keyMetaInfoFromBucket.get(0);

		if (s3ObjectSummary.getSize() < length)
			throw new IllegalArgumentException(
					"Requested object is smaller than the length of data expected: Requested object size: "
							+ s3ObjectSummary.getSize()
							+ ". Requested number of bytes: "
							+ length
							+ ". Object key: "
							+ String.format(DIRECTORY_RANGE_FETCH_KEY_FORMAT,
									s3DirectoryPrefix, fileName));

		if (s3ObjectSummary.getSize() < (offset + length) - 1)
			throw new IllegalArgumentException(
					"Requested object is smaller than the length of data expected: Requested object size: "
							+ s3ObjectSummary.getSize()
							+ ". Requested number of bytes: "
							+ (offset + length - 1)
							+ ". Object key: "
							+ String.format(DIRECTORY_RANGE_FETCH_KEY_FORMAT,
									s3DirectoryPrefix, fileName));

		byte[] data = downloadPiece(bucketName, String.format(
				DIRECTORY_RANGE_FETCH_KEY_FORMAT, s3DirectoryPrefix, fileName),
				Long.valueOf(offset), Long.valueOf(offset + length), true);

		return data;
	}

	public static byte[] downloadCompleteFile(String bucketName, String key)
			throws TruncatedPieceReadException, S3ObjectNotFoundException,
			FetchSizeExceededException, S3FetchException {

		if (Strings.isNullOrEmpty(bucketName) || Strings.isNullOrEmpty(key))
			throw new IllegalArgumentException(
					"bucket-name , and file-key can not be empty or null");
		else {
			bucketName = bucketName.trim();
			key = key.trim();
		}

		byte[] data = downloadPiece(bucketName, key, null, null, false);
		return data;
	}

	/*
	 * endByteIndex is exclusive byte index in S3 also starts from 0 Range set
	 * in get request of S3 has both the endpoints inclusive
	 */

	public static byte[] downloadPiece(String bucketName, String key,
			Long startByteIndex, Long endByteIndex,
			boolean isDirectoryFetch) throws TruncatedPieceReadException,
			S3ObjectNotFoundException, FetchSizeExceededException,
			S3FetchException {

		if (Strings.isNullOrEmpty(bucketName) || Strings.isNullOrEmpty(key))
			throw new IllegalArgumentException(
					"bucket-name , and file-key can not be empty or null");
		else {
			bucketName = bucketName.trim();
			key = key.trim();
		}

		long length = 0;
		List<S3ObjectSummary> objList = getKeyMetaInfoFromBucket(bucketName,
				key);
		int numObjsInBucket = keyExistsInBucket(bucketName, key, objList);
		if (numObjsInBucket == 0)
			throw new S3ObjectNotFoundException("No object with key: " + key
					+ " in the bucket: " + bucketName + " can be found");
		else {
			logger.info(numObjsInBucket + " objects found with the key: " + key
					+ " in bucket " + bucketName);
			if (!isDirectoryFetch) {
				if (numObjsInBucket > 1)
					throw new IllegalArgumentException(
							"More than one file is associated with the given key: "
									+ key);
			}
			S3ObjectSummary s3ObjectSummary = objList.get(0);
			length = s3ObjectSummary.getSize();
		}

		GetObjectRequest req = new GetObjectRequest(bucketName, key);

		if (startByteIndex != null && endByteIndex != null) {
			length = (endByteIndex - startByteIndex); // This is placed before
														// decrementing the
														// endIndex to
														// accurately calculate
														// the number of bytes
														// to be fetched
			endByteIndex = endByteIndex - 1; // This is to make endByteIndex
												// exclusive
			if (startByteIndex > endByteIndex)
				throw new IllegalArgumentException(
						"startByteIndex should be smaller than endByteIndex to fetch the legitimate data bytes");
			else {
				logger.info("Fetching " + length
						+ " bytes data of a piece from S3");
				req.setRange(startByteIndex, endByteIndex);
			}
		} else
			logger.info("Range has not been provided for this download from cloud. Whole "
					+ key + " will be downloaded from bucket: " + bucketName);

		if (length > PIECE_LENGTH * 1024)
			throw new FetchSizeExceededException(
					"Max fetch size exceeded. Consider chunking the download in parts. Max allowed size is piece size: "
							+ PIECE_LENGTH * 1024+"bytes, "+PIECE_LENGTH+" kb");
		ByteBuffer buffer = ByteBuffer.allocate((int) length);
		byte[] holder = new byte[(int) length];
		S3Object piece = s3.getObject(req);
		logger.info("Downloading a piece of size " + (length / 1024)
				+ "kb from cloud for content type: "
				+ piece.getObjectMetadata().getContentType());

		int rem = 0;

		try {
			for (rem = piece.getObjectContent().read(holder); rem != -1; rem = piece
					.getObjectContent().read(holder)) {
				logger.debug("fetched: " + rem + " bytes from cloud for key: "
						+ key);
				buffer.put(Arrays.copyOfRange(holder, 0, rem));
			}
		} catch (IOException e) {
			logger.error(
					"IOException while fetching the data from cloud for key: "
							+ key, e);
			throw new S3FetchException(
					"Exception occurred while fetching the data from the cloud");
		}
		logger.info("Total bytes read from cloud: " + buffer.position()
				+ " for key: " + key);

		if (buffer.position() != length)
			throw new TruncatedPieceReadException(
					"Number of bytes expected to be read: " + length
							+ ". Number of bytes actually read: "
							+ buffer.position());

		return buffer.array();
	}
}
