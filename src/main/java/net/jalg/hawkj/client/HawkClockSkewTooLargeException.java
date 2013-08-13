package net.jalg.hawkj.client;


public class HawkClockSkewTooLargeException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private long suggestedTimestamp;
	
	public HawkClockSkewTooLargeException(long suggestedTimestamp) {
		super("The server considers client and server clock skew too large. Server timestamp is " + suggestedTimestamp);
		this.suggestedTimestamp = suggestedTimestamp;
	}

	public long getSuggestedTimestamp() {
		return suggestedTimestamp;
	}

	
}
