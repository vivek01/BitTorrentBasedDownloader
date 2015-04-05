package com.p2p.peercds.common;

public class S3ObjectNotFoundException extends Exception {
	private String message;

	public S3ObjectNotFoundException() {
		super();
	}
	
	public S3ObjectNotFoundException(String message) {
        super(message);
        this.message = message;
    }
 
    public S3ObjectNotFoundException(Throwable cause) {
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
