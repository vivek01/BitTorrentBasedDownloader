package com.rest.service.mappers;

public class CreateTorrentResponseMapper{


	private String success;
	private String message;
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getSuccess() {
		return success;
	}

	public void setSuccess(String success) {
		this.success = success;
	}
	
	private String defaultDirectory;

	public String getDefaultDirectory() {
		return defaultDirectory;
	}

	public void setDefaultDirectory(String defaultDirectory) {
		this.defaultDirectory = defaultDirectory;
	}
}
