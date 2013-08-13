package net.jalg.hawkj.client;

import net.jalg.hawkj.AuthHeaderParsingException;


public class HawkSecurityException extends RuntimeException {

	public HawkSecurityException(String message) {
		super(message);
	}

	public HawkSecurityException(String message, Throwable cause) {
		super(message,cause);
	}
	
	
}
