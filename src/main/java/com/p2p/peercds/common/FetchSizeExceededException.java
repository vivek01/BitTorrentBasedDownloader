package com.p2p.peercds.common;

public class FetchSizeExceededException extends Exception {

	private String message;

	public FetchSizeExceededException() {
		super();
	}
	
	public FetchSizeExceededException(String message) {
        super(message);
        this.message = message;
    }
 
    public FetchSizeExceededException(Throwable cause) {
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
