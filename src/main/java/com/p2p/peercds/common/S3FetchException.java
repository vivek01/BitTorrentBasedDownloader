package com.p2p.peercds.common;

public class S3FetchException extends Exception {

	private String message;

	public S3FetchException() {
		super();
	}
	
	public S3FetchException(String message) {
        super(message);
        this.message = message;
    }
 
    public S3FetchException(Throwable cause) {
        super(cause);
    }
	
	
	 @Override
	    public String toString() {
	        return message;
	    }
	 
	    @Override
	    public String getMessage() {
	        return message;
	    }
}
