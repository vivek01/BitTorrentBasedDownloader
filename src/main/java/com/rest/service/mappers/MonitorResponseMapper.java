package com.rest.service.mappers;

public class MonitorResponseMapper {

	private String uuid;
	private String fileName;
	private String status;
	private String progress;
	private String uploadSpeed;
	private String downloadSpeed;
	private String peers;
	private String seeds;
	private String eta;
	private String size;
	private boolean paused;
	private boolean error;
	private String elapsedTime;
	
	
	public String getElapsedTime() {
		return elapsedTime;
	}
	public void setElapsedTime(String elapsedTime) {
		this.elapsedTime = elapsedTime;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getProgress() {
		return progress;
	}
	public void setProgress(String progress) {
		this.progress = progress;
	}
	public String getUploadSpeed() {
		return uploadSpeed;
	}
	public void setUploadSpeed(String uploadSpeed) {
		this.uploadSpeed = uploadSpeed;
	}
	public String getDownloadSpeed() {
		return downloadSpeed;
	}
	public void setDownloadSpeed(String downloadSpeed) {
		this.downloadSpeed = downloadSpeed;
	}
	public String getPeers() {
		return peers;
	}
	public void setPeers(String peers) {
		this.peers = peers;
	}
	public String getSeeds() {
		return seeds;
	}
	public void setSeeds(String seeds) {
		this.seeds = seeds;
	}
	public String getEta() {
		return eta;
	}
	public void setEta(String eta) {
		this.eta = eta;
	}
	public String getSize() {
		return size;
	}
	public void setSize(String size) {
		this.size = size;
	}
	public boolean isPaused() {
		return paused;
	}
	public void setPaused(boolean paused) {
		this.paused = paused;
	}
	public boolean isError() {
		return error;
	}
	public void setError(boolean error) {
		this.error = error;
	}
}
