package com.rest.service.mappers;

public class GenericTorrentResponseMapper {

	private String success;
	private String message;
	private MonitorResponseMapper metadata;
	public MonitorResponseMapper getMetadata() {
		return metadata;
	}

	public void setMetadata(MonitorResponseMapper metadata) {
		this.metadata = metadata;
	}

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
}
