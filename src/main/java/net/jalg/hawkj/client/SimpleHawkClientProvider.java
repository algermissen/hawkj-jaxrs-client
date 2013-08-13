package net.jalg.hawkj.client;

import java.net.URI;

import net.jalg.hawkj.ext.HawkCredentials;

public class SimpleHawkClientProvider implements HawkClientProvider {
	
	HawkCredentials credentials;
	long offset;

	public SimpleHawkClientProvider(HawkCredentials credentials) {
		super();
		this.credentials = credentials;
		this.offset = 0;
	}

	@Override
	public HawkCredentials getCredentials(URI uri) {
		return credentials;
	}

	@Override
	public long getClockOffset(URI uri) {
		return offset;
	}
	
	public void setClockOffset(long offset) {
		this.offset = offset;
	}

}
