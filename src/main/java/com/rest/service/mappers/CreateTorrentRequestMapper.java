package com.rest.service.mappers;

public class CreateTorrentRequestMapper {

	private String fileName;
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public boolean isCloud() {
		return cloud;
	}
	public void setCloud(boolean cloud) {
		this.cloud = cloud;
	}
	private boolean cloud;
}
