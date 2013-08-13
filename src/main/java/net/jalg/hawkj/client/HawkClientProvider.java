package net.jalg.hawkj.client;

import java.net.URI;

import net.jalg.hawkj.ext.HawkCredentials;

public interface HawkClientProvider {
	
	public HawkCredentials getCredentials(URI uri);

	public long getClockOffset(URI uri);
	
}
