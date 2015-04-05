package com.p2p.peercds.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.p2p.peercds.client.Client;
import com.p2p.peercds.client.Client.ClientState;
import com.p2p.peercds.client.peer.SharingPeer;
import com.p2p.peercds.common.Torrent;
import com.rest.service.mappers.ClientMetadata;
import com.rest.service.mappers.CreateTorrentResponseMapper;
import com.rest.service.mappers.DefaultDirectoryRequestMapper;
import com.rest.service.mappers.FileMapper;
import com.rest.service.mappers.GenericResponseStatusMapper;
import com.rest.service.mappers.GenericTorrentResponseMapper;
import com.rest.service.mappers.MonitorResponseMapper;

public class ClientWrapper {

	private static final Logger logger =
			LoggerFactory.getLogger(ClientWrapper.class);

	private HashMap<String, ClientMetadata> clientMap;

	private static ClientWrapper clientWrapper;

	private static String DEFAULT_OUTPUT_DIRECTORY = "/Desktop/torrentdir/";

	private static String TORRENT_METADATA_DIRECTORY = "/fileMap.txt";
	
	private static int MINUTES_IN_AN_HOUR = 60;
    private static int SECONDS_IN_A_MINUTE = 60;

	private ClientWrapper() {

	}

	public static ClientWrapper getClientWrapper() {

		if(clientWrapper == null){

			clientWrapper = new ClientWrapper();
			logger.info("ClientWrapper(): Initialized ClientWrapper");
			
			String userHome = System.getProperty("user.home"); 
			DEFAULT_OUTPUT_DIRECTORY = userHome+DEFAULT_OUTPUT_DIRECTORY;
			TORRENT_METADATA_DIRECTORY = userHome+TORRENT_METADATA_DIRECTORY;

			clientWrapper.readMetadataFromFile();
			logger.info("ClientWrapper(): Done reading state from metadata file");

		}
		return clientWrapper;
	}

	public HashMap<String, ClientMetadata> getClientMap() {
		return clientMap;
	}

	public void setClientMap(HashMap<String, ClientMetadata> clientMap) {
		this.clientMap = clientMap;
	}

	public static String getDEFAULT_OUTPUT_DIRECTORY() {
		return DEFAULT_OUTPUT_DIRECTORY;
	}

	public static void setDEFAULT_OUTPUT_DIRECTORY(String dEFAULT_OUTPUT_DIRECTORY) {

		DEFAULT_OUTPUT_DIRECTORY = dEFAULT_OUTPUT_DIRECTORY;
		logger.info("setDEFAULT_OUTPUT_DIRECTORY(): Default directory set to: "+dEFAULT_OUTPUT_DIRECTORY);
	}

	public synchronized CreateTorrentResponseMapper createTorrent(String fileName, String trackerURL){

		CreateTorrentResponseMapper response = new CreateTorrentResponseMapper();

		String torrentName = null;
		
		try{
			

			TorrentMain.createTorrent(DEFAULT_OUTPUT_DIRECTORY, fileName, trackerURL);
			logger.info("createTorrent(): Torrent file created in directory: "+getDEFAULT_OUTPUT_DIRECTORY());

		} catch (IllegalArgumentException e){

			logger.info("createTorrent(): Could not create "+fileName+" in directory: "+getDEFAULT_OUTPUT_DIRECTORY()+" Error: "+e.getMessage());
			response.setSuccess("false");
			response.setMessage("Torrent Start Failed: Data file not found in the default directory");
			return response;

		} catch (Exception e){
			
			logger.info("createTorrent(): Could not create "+fileName+" in directory: "+getDEFAULT_OUTPUT_DIRECTORY()+" Error: "+e.getMessage());
			response.setSuccess("false");
			response.setMessage("Torrent Start Failed: Internal Error");
			return response;
		}

		File file = new File(DEFAULT_OUTPUT_DIRECTORY+fileName);
		
		if(file.isFile()){
			torrentName = fileName.split("\\.(?=[^\\.]+$)")[0];
			torrentName = torrentName +".torrent";
		} else {
			torrentName = fileName +".torrent";
		}
		
		GenericTorrentResponseMapper downloadTorrentResponse = this.downloadTorrent(torrentName);
		if(downloadTorrentResponse.getSuccess().equals("false")){
			response.setSuccess("false");
			response.setMessage(downloadTorrentResponse.getMessage());
			return response;
		}
				
		logger.info("createTorrent(): Starting seeding of torrent: "+torrentName);
		
		response.setSuccess("true");
		response.setMessage("Torrent created successfully");

		return response;

	}

	public GenericResponseStatusMapper deleteTorrent(String uuid){

		Client client = null;
		GenericResponseStatusMapper genericresponse = new GenericResponseStatusMapper();
		
		ClientMetadata clientMetadata= clientMap.get(uuid);
		if(clientMetadata == null){
			logger.info("deleteTorrent(): Could not find torrent with ID:"+uuid+" in client map ");
			genericresponse.setSuccess("false");
			genericresponse.setMessage("Could not find torrent");
			
			return genericresponse;
		}
			
		client = clientMetadata.getClient();

		logger.info("deleteTorrent(): Deleting torrent with ID:"+uuid);
		new Thread(new Client.ClientShutdown(client, null)).start();
		
		clientMap.remove(uuid);

		logger.info("deleteTorrent(): Deleted torrent with ID:"+uuid);
		
		writeMetadataToFile();

		genericresponse.setSuccess("true");
		genericresponse.setMessage("Torrent deleted");
		return genericresponse;
	}

	public GenericResponseStatusMapper setDefaultDirectory(String directory){

		GenericResponseStatusMapper response = new GenericResponseStatusMapper();
		
		if(directory != null && !directory.equalsIgnoreCase("")){

			

			if(!directory.endsWith("/")){
				directory = directory.concat("/");
			}

			//directory = directory.replaceAll("/", "//");

			File file = new File(directory);
			if(!file.exists() || !file.isDirectory()){
				response.setSuccess("false");
				response.setMessage("Directory not found!!");
				return response;
			}
			
			setDEFAULT_OUTPUT_DIRECTORY(directory);

			logger.info("setDefaultDirectory(): Default Directory set to: "+directory);
			
			response.setSuccess("true");
			response.setMessage("Default directory set sucessfully");
			return response;
		}
		else{
			
			logger.info("setDefaultDirectory(): Bad Request");
			
			response.setSuccess("false");
			response.setMessage("Default directory not be set. Please enter default directory");
			return response;
		}

	}

	public DefaultDirectoryRequestMapper getDefaultDirectory(){

		
		DefaultDirectoryRequestMapper responseMapper = new DefaultDirectoryRequestMapper();
		responseMapper.setDefaultDirectory(DEFAULT_OUTPUT_DIRECTORY);
		return responseMapper;
	}

	public List<MonitorResponseMapper> getTorrents(){

		List<MonitorResponseMapper> responselist = new ArrayList<MonitorResponseMapper>();

		Iterator<Entry<String, ClientMetadata>> iterator = clientMap.entrySet().iterator();

//		logger.info("getTorrents(): Iterating client map with "+clientMap.size()+ " entries");
		while(iterator.hasNext()){

			Entry<String, ClientMetadata> clientEntry = iterator.next();
			ClientMetadata clientMetadata = clientEntry.getValue();


			if(!clientMetadata.isPaused() && !clientMetadata.isError()){
				
				
				Client client = clientMetadata.getClient();
				
//				logger.info("getTorrents(): Retrieving metadata for torrent: "+clientMetadata.getTorrentName());
				MonitorResponseMapper metadata = new MonitorResponseMapper();

				// calculate download and upload speed
				float dl = 0;
				float ul = 0;
				int noOfSeeds = 0;
				Iterator<SharingPeer> connectedIterator = client.getConnected().values().iterator();
				while(connectedIterator.hasNext()) {
					SharingPeer peer = connectedIterator.next();
					dl += peer.getDLRate().get();
					ul += peer.getULRate().get();
					if(peer.getAvailablePieces().cardinality() == client.getTorrent().getPieceCount()){
						noOfSeeds++;
					}
				}
				
				dl= dl + client.getTorrent().getCloudDLRate();
				
				logger.info("getTorrents(): DL: "+dl/1024);
				metadata.setDownloadSpeed(String.format("%.2f", dl/1024.0)+" kB/s");
				metadata.setUploadSpeed(String.format("%.2f", ul/1024.0)+" kB/s");

				// set size
				
				
				double size = client.getTorrent().getSize()/1024;
				logger.info("getTorrents(): Size: "+size);
				if(size > 1024*1024){
					metadata.setSize(String.format("%.2f",size/(1024*1024))+"GB");
				} else if(size > 1024){
					metadata.setSize(String.format("%.2f",(size/1024))+"MB");
				} else {
					metadata.setSize(String.format("%.2f",(size))+"kB");
				}
				//set eta
				

				//set progress percent
				metadata.setProgress(String.format("%.2f", client.getTorrent().getCompletion()));

				// set peers
				metadata.setPeers(String.valueOf(client.getPeers().size()));

				//set name
				metadata.setFileName(clientMetadata.getTorrentName());

				//set seeds
				metadata.setSeeds(String.valueOf(noOfSeeds));

				//set status
				ClientState state = client.getState();
				if(state.equals(ClientState.SHARING)||state.equals(ClientState.VALIDATING)||state.equals(ClientState.WAITING)){

					metadata.setStatus("Downloading");
					if(dl == 0.00){
						metadata.setEta("Infinity");
					} else {
						
						logger.info("getTorrents(): ETA in seconds: "+size/(dl/1024));
						metadata.setEta(timeConversion((int)(size/(dl/1024))));
					}
					
				} else {

					metadata.setStatus("Seeding");
					metadata.setEta("Infinity");
				}

				metadata.setUuid(clientEntry.getKey());

				metadata.setPaused(clientMetadata.isPaused());

				metadata.setError(clientMetadata.isError());
				
				long startTime = client.getStartTime();
				
				long timeElapsed = System.currentTimeMillis() - startTime;
				
				logger.info("getTorrents(): startTime: "+startTime);
				logger.info("getTorrents(): timeElapsed: "+System.currentTimeMillis());
				logger.info("getTorrents(): timeElapsed: "+timeElapsed);
				
				metadata.setElapsedTime(timeConversion((int)(timeElapsed/1000)));
				
				responselist.add(metadata);
			}
			else {
				
//				logger.info("getTorrents(): Skipping retrieval of metadata for torrent: "+clientMetadata.getTorrentName());
				MonitorResponseMapper metadata = new MonitorResponseMapper();
				metadata.setError(clientMetadata.isError());
				metadata.setPaused(clientMetadata.isPaused());
				metadata.setFileName(clientMetadata.getTorrentName());
				metadata.setUuid(clientEntry.getKey());
				responselist.add(metadata);
			}

		}
		return responselist;
	}

	public GenericTorrentResponseMapper startTorrent(String uuid){

		GenericTorrentResponseMapper response = new GenericTorrentResponseMapper();

		ClientMetadata clientMetadata = clientMap.get(uuid);
		if(clientMetadata == null){
			logger.info("startTorrent(): Could not find torrent with ID:"+uuid+" in client map ");
			response.setSuccess("false");
			response.setMessage("Could not find torrent");
			
			return response;
		}

		if(clientMetadata.isPaused()){

			logger.info("startTorrent(): Starting torrent :"+clientMetadata.getTorrentName());
			Client c = null;
			c = ClientMain.startTorrent(clientMetadata.getTorrentDirectory(), clientMetadata.getTorrentName());

			if(c!=null){
				clientMetadata.setError(false);
				clientMetadata.setPaused(false);
				clientMetadata.setClient(c);

				logger.info("startTorrent(): Started torrent :"+clientMetadata.getTorrentName());
				writeMetadataToFile();

				response.setSuccess("true");
				response.setMessage("Torrent Started");

				return response;

			} else {

				logger.info("startTorrent(): Could not find torrent :"+clientMetadata.getTorrentName());
				clientMetadata.setError(true);
				response.setSuccess("false");
				response.setMessage("Could not initialize torrent: File "+clientMetadata.getTorrentName()+" not found in "+clientMetadata.getTorrentDirectory()+" directory!");

				return response;
			}
		}
		else{

			logger.info("startTorrent(): Torrent :"+clientMetadata.getTorrentName()+" already running");
			response.setSuccess("false");
			response.setMessage("Torrent already running");

			return response;
		}

	}

	public GenericResponseStatusMapper pauseTorrent(String uuid){

		Client client = null;
		GenericResponseStatusMapper genericresponse = new GenericResponseStatusMapper();
		
		ClientMetadata clientMetadata= clientMap.get(uuid);
		
		if(clientMetadata == null){
			logger.info("pauseTorrent(): Could not find torrent with ID:"+uuid+" in client map ");
			genericresponse.setSuccess("false");
			genericresponse.setMessage("Could not find torrent");
			
			return genericresponse;
		}
		
		client = clientMetadata.getClient();

		if(!clientMetadata.isPaused()){

			logger.info("pauseTorrent(): Pausing torrent :"+clientMetadata.getTorrentName());
			new Thread(new Client.ClientShutdown(client, null)).start();
			clientMetadata.setPaused(true);

			logger.info("startTorrent(): Paused torrent :"+clientMetadata.getTorrentName());
			writeMetadataToFile();

			genericresponse.setSuccess("true");
			genericresponse.setMessage("Torrent paused");
		} 
		else {

			logger.info("startTorrent(): Torrent :"+clientMetadata.getTorrentName()+" already paused");
			genericresponse.setSuccess("true");
			genericresponse.setMessage("Torrent already paused");
		}
		
		return genericresponse;
	}

	public synchronized GenericTorrentResponseMapper downloadTorrent(String torrentName){

		return downloadTorrent(getDEFAULT_OUTPUT_DIRECTORY(), torrentName, false);
	}


	public GenericTorrentResponseMapper downloadTorrent(String defaultDirectory, String torrentName, boolean paused){

		String info_hash = null;
		GenericTorrentResponseMapper response = new GenericTorrentResponseMapper();

		try {
			
			if(!torrentName.split("\\.(?=[^\\.]+$)")[1].equals("torrent")){
				logger.info("downloadTorrent(): File is not a torrent file.");
				response.setSuccess("false");
				response.setMessage("Torrent Start Failed: File is not torrent file");
				return response;
			}
			
			Torrent torrent = Torrent.load(new File(defaultDirectory+torrentName));
			logger.info("downloadTorrent(): Checking info_hash for duplicate torrent: "+torrentName);
			info_hash = new String(torrent.getInfoHash(),"ISO-8859-1");
			//System.out.println("info-hash:"+info_hash);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.info("downloadTorrent(): File not found in the default directory. Error: "+e.getMessage());
			response.setSuccess("false");
			response.setMessage("Torrent Start Failed: File not found in the default directory");
			return response;
			//e.printStackTrace();
		}

		Iterator<Entry<String, ClientMetadata>> iterator = clientMap.entrySet().iterator();

		while(iterator.hasNext()){

			Entry<String, ClientMetadata> clientEntry = iterator.next();
			ClientMetadata clientMetadata = clientEntry.getValue();
			if(clientMetadata.getInfoHash().equals(info_hash)){

				logger.info("downloadTorrent(): Torrent "+torrentName+"already running in the client");
				response.setSuccess("false");
				response.setMessage("Torrent already running in the client");
				return response;
			}
		}

		logger.info("downloadTorrent(): Starting download of torrent: "+torrentName);
		Client c = null;
		c = ClientMain.startTorrent(defaultDirectory, torrentName);

		if(c == null){

			
			logger.info("downloadTorrent(): Torrent Start Failed: File not found in the default directory");
			response.setSuccess("false");
			response.setMessage("Torrent Start Failed: File not found in the default directory");
			return response;

		} else {

			//add to map
			ClientMetadata clientMetadata = new ClientMetadata();
			clientMetadata.setClient(c);
			clientMetadata.setTorrentDirectory(defaultDirectory);
			clientMetadata.setTorrentName(torrentName);
			clientMetadata.setPaused(paused);
			String uuid = UUID.randomUUID().toString();
			clientMetadata.setInfoHash(info_hash);
			clientMap.put(uuid, clientMetadata);

			// write to file
			writeMetadataToFile();

			logger.info("downloadTorrent(): Torrent "+torrentName+" Started");

			response.setSuccess("true");
			response.setMessage("Torrent Started");

			return response;
		}
	}

	private void writeMetadataToFile(){

		ObjectMapper mapper = new ObjectMapper();
		try {

			logger.info("writeMetadataToFile(): Writing client map to metadata file: "+TORRENT_METADATA_DIRECTORY);
			FileMapper fileMapper = new FileMapper();
			fileMapper.setDefaultDirectory(getDEFAULT_OUTPUT_DIRECTORY());
			fileMapper.setClientMap(clientMap);

			File file = new File(TORRENT_METADATA_DIRECTORY);
			
			if(file.exists()){
				logger.info("writeMetadataToFile(): Metadata file already exists.. Updating file");
				file.setWritable(true);
				file.delete();
			}

			mapper.writeValue( new File(TORRENT_METADATA_DIRECTORY), fileMapper);

			logger.info("writeMetadataToFile(): Metadata file "+TORRENT_METADATA_DIRECTORY+" Updated");
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			logger.debug("writeMetadataToFile(): "+e.getMessage());
			//e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			logger.debug("writeMetadataToFile(): "+e.getMessage());
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.debug("writeMetadataToFile(): "+e.getMessage());
			//e.printStackTrace();
		}
	}

	private void readMetadataFromFile(){

		ObjectMapper mapper = new ObjectMapper();
		try {

			File file = new File(TORRENT_METADATA_DIRECTORY);
			logger.info("readMetadataFromFile(): Reading metadata from file "+TORRENT_METADATA_DIRECTORY);
			if(file.exists()){

				logger.info("readMetadataFromFile(): Found metadata file at path: "+TORRENT_METADATA_DIRECTORY);
				this.clientMap = new HashMap<String, ClientMetadata>();

				FileMapper fileMapper = mapper.readValue(file, FileMapper.class);
				setDEFAULT_OUTPUT_DIRECTORY(fileMapper.getDefaultDirectory());
				logger.info("readMetadataFromFile(): ClientWrapper default directory set to: "+fileMapper.getDefaultDirectory());

				Iterator<Entry<String, ClientMetadata>> iterator = fileMapper.getClientMap().entrySet().iterator();

				while(iterator.hasNext()){

					Entry<String, ClientMetadata> clientEntry = iterator.next();
					ClientMetadata clientMetadata = clientEntry.getValue();

					Client c = null;
					if(!clientMetadata.isPaused()){

						c = ClientMain.startTorrent(clientMetadata.getTorrentDirectory(), clientMetadata.getTorrentName());
						logger.info("readMetadataFromFile(): Initializing torrent : "+clientMetadata.getTorrentDirectory()+clientMetadata.getTorrentName());

						if(c != null){

							logger.info("readMetadataFromFile(): Torrent started : "+clientMetadata.getTorrentDirectory()+clientMetadata.getTorrentName());
							clientMetadata.setClient(c);
							
						} else {

							clientMetadata.setError(true);
							logger.info("readMetadataFromFile(): Could not initialize torrent : "+clientMetadata.getTorrentDirectory()+clientMetadata.getTorrentName()+" : File not found!");
						}

					} else{

						logger.info("readMetadataFromFile(): Skipping Initialization for paused torrent : "+clientMetadata.getTorrentDirectory()+clientMetadata.getTorrentName());
					}
					
					logger.info("readMetadataFromFile(): Pushing torrent to Map : "+clientMetadata.getTorrentDirectory()+clientMetadata.getTorrentName());
					this.clientMap.put(clientEntry.getKey(), clientEntry.getValue());

				}

			} else {
				logger.info("readMetadataFromFile(): Could not find metadata file at path: "+TORRENT_METADATA_DIRECTORY);
				this.clientMap = new HashMap<String, ClientMetadata>();
			}


		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			logger.debug("readMetadataFromFile(): "+e.getMessage());
			//e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			logger.debug("readMetadataFromFile(): "+e.getMessage());
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.debug("readMetadataFromFile(): "+e.getMessage());
			//e.printStackTrace();
		}
	}
	
    
	private static String timeConversion(int totalSeconds) {
		
		logger.info("timeConversion(): totalseconds: "+totalSeconds);
	    
        int hours = totalSeconds / MINUTES_IN_AN_HOUR / SECONDS_IN_A_MINUTE;
        int minutes = (totalSeconds - (hoursToSeconds(hours)))
                / SECONDS_IN_A_MINUTE;
        int seconds = totalSeconds
                - ((hoursToSeconds(hours)) + (minutesToSeconds(minutes)));

        if(hours > 0){
        	return hours + " hrs " + minutes + " min " + seconds + " s";
        } else if(minutes > 0){
        	return minutes + " min " + seconds + " s";
        } else {
        	return seconds + " sec";
        }
    }
	
	private static int hoursToSeconds(int hours) {
        return hours * MINUTES_IN_AN_HOUR * SECONDS_IN_A_MINUTE;
    }

    private static int minutesToSeconds(int minutes) {
        return minutes * SECONDS_IN_A_MINUTE;
    }

}
