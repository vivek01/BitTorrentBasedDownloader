package com.rest.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.p2p.peercds.cli.ClientWrapper;
import com.rest.service.mappers.CreateTorrentResponseMapper;
import com.rest.service.mappers.DefaultDirectoryRequestMapper;
import com.rest.service.mappers.GenericResponseStatusMapper;
import com.rest.service.mappers.GenericTorrentResponseMapper;
import com.rest.service.mappers.MonitorResponseMapper;
import com.sun.jersey.api.view.Viewable;

@Path("/service")
public class TorrentService {

	private static final Logger logger =
			LoggerFactory.getLogger(TorrentService.class);
	private static ClientWrapper clientWrapper;
	
	static{
		ClientWrapper.getClientWrapper();
	}
	
	public TorrentService(){
		
		super();
		if(clientWrapper == null){
			clientWrapper = ClientWrapper.getClientWrapper();
		}
	}
	
	@GET
	@Path("/defaultdirectory")
	@Produces(MediaType.APPLICATION_JSON)
	public String getDefaultDirectory(){
		
		String response = null;
		DefaultDirectoryRequestMapper responseMapper = clientWrapper.getDefaultDirectory();
		try {
			response = (new ObjectMapper().writeValueAsString(responseMapper));
		} catch (JsonGenerationException e) {
			logger.debug("getDefaultDirectory(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (JsonMappingException e) {
			logger.debug("getDefaultDirectory(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			logger.debug("getDefaultDirectory(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		return response;
	}
	
	@POST
	@Path("/defaultdirectory")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String setDefaultDirectory(String requestJson){
		
		ObjectMapper mapper = new ObjectMapper();
		String responseString = null;
		
		Map<String, String> requestMap = null;
		try {
			
			requestMap = mapper.readValue(requestJson, Map.class);

			GenericResponseStatusMapper response = clientWrapper.setDefaultDirectory(requestMap.get("defaultDirectory"));
			
			responseString = mapper.writeValueAsString(response);
		} catch (JsonParseException e) {
			logger.debug("setDefaultDirectory(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (JsonMappingException e) {
			logger.debug("setDefaultDirectory(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			logger.debug("setDefaultDirectory(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
				
		return responseString;
	}
	
	@POST
	@Path("/createtorrent")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String createTorrent(String requestJson){
		
		String responseString = null;
		ObjectMapper mapper = new ObjectMapper();
		
		Map<String, String> requestMap = null;
		String request = null;
		
		try {
			
			requestMap = mapper.readValue(requestJson, Map.class);
			
			CreateTorrentResponseMapper response = clientWrapper.createTorrent( requestMap.get("filename"),requestMap.get("trackerurl"));
			
			responseString = mapper.writeValueAsString(response);
			
		} catch (JsonParseException e) {
			logger.debug("createTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e1.printStackTrace();
		} catch (JsonMappingException e) {
			logger.debug("createTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e1.printStackTrace();
		} catch (IOException e) {
			logger.debug("createTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e1.printStackTrace();
		}
				
		return responseString;
	}
	
	@GET
	@Path("/gettorrents")
	@Produces(MediaType.APPLICATION_JSON)
	public String getTorrents(){
		
		ObjectMapper mapper = new ObjectMapper();
		String responseString = null;
		
		List<MonitorResponseMapper> responselist = clientWrapper.getTorrents();
		
		try {
			responseString = mapper.writeValueAsString(responselist);
		} catch (JsonGenerationException e) {
			logger.debug("getTorrents(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (JsonMappingException e) {
			logger.debug("getTorrents(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			logger.debug("getTorrents(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		return responseString;
	}
	
	@POST
	@Path("/starttorrent")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String startTorrent(String requestJson){
		
		String responseString = null;
		ObjectMapper mapper = new ObjectMapper();
		
		Map<String, String> requestMap = null;
		try {
			
			requestMap = mapper.readValue(requestJson, Map.class);
						
			GenericTorrentResponseMapper response = clientWrapper.startTorrent(requestMap.get("uuid"));
			
			responseString = mapper.writeValueAsString(response);
			
		} catch (JsonParseException e) {
			logger.debug("startTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (JsonMappingException e) {
			logger.debug("startTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			logger.debug("startTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		return responseString;
	}
 
	@POST
	@Path("/pausetorrent")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String pauseTorrent(String requestJson){
		
		String responseString = null;
		ObjectMapper mapper = new ObjectMapper();
		
		Map<String, String> requestMap = null;
		try {
			
			requestMap = mapper.readValue(requestJson, Map.class);
			
			GenericResponseStatusMapper response = clientWrapper.pauseTorrent(requestMap.get("uuid"));
			
			responseString = mapper.writeValueAsString(response);
			
		} catch (JsonParseException e) {
			logger.debug("pauseTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (JsonMappingException e) {
			logger.debug("pauseTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			logger.debug("pauseTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
				
		return responseString;
	}
	
	@POST
	@Path("/deletetorrent")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String deleteTorrent(String requestJson){
		
		String responseString = null;
		ObjectMapper mapper = new ObjectMapper();
		
		
		Map<String, String> requestMap = null;
		try {
			
			requestMap = mapper.readValue(requestJson, Map.class);
						
			GenericResponseStatusMapper response = clientWrapper.deleteTorrent(requestMap.get("uuid"));
			
			responseString = mapper.writeValueAsString(response);
			
		} catch (JsonParseException e) {
			logger.debug("deleteTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (JsonMappingException e) {
			logger.debug("deleteTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			logger.debug("deleteTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		
		return responseString;
	}
	
	@POST
	@Path("/downloadtorrent")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String downloadTorrent(String requestJson){
		
		String responseString = null;
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> requestMap = null;
		String request = null;
		
		try {
			//System.out.println(requestJson);
			requestMap = mapper.readValue(requestJson, Map.class);
			
			GenericTorrentResponseMapper response = clientWrapper.downloadTorrent(requestMap.get("filename"));
			
			responseString = mapper.writeValueAsString(response);
		} catch (JsonParseException e) {
			logger.debug("downloadTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (JsonMappingException e) {
			logger.debug("downloadTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			logger.debug("downloadTorrent(): "+e.getMessage());
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		return responseString;

	}
	
	@GET
	@Path("/view")
	@Produces(MediaType.TEXT_HTML)

	public Response downloadTorrent(){

 return Response.ok(new Viewable("/index.html")).build();
	    
}
}
