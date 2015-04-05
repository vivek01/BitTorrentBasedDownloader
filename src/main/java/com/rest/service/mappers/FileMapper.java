package com.rest.service.mappers;

import java.util.HashMap;

public class FileMapper {

	private String defaultDirectory;
	private HashMap<String, ClientMetadata> clientMap;
	public String getDefaultDirectory() {
		return defaultDirectory;
	}
	public void setDefaultDirectory(String defaultDirectory) {
		this.defaultDirectory = defaultDirectory;
	}
	public HashMap<String, ClientMetadata> getClientMap() {
		return clientMap;
	}
	public void setClientMap(HashMap<String, ClientMetadata> clientMap) {
		this.clientMap = clientMap;
	}
	
}
