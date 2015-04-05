package com.p2p.peercds.common;

public class TruncatedPieceReadException extends Exception {

	private String message;

	public TruncatedPieceReadException() {
		super();
	}
	
	public TruncatedPieceReadException(String message) {
        super(message);
        this.message = message;
    }
 
    public TruncatedPieceReadException(Throwable cause) {
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
