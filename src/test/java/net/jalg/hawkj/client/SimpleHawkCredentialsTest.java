package net.jalg.hawkj.client;

import static org.junit.Assert.*;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.jalg.hawkj.Algorithm;
import net.jalg.hawkj.client.ClientLoggingFilter;
import net.jalg.hawkj.client.HawkClientProvider;
import net.jalg.hawkj.ext.HawkCredentials;

import org.junit.Test;

public class SimpleHawkCredentialsTest {

	// Test currently fais do to dependency issues @Test
	// It is just meant for experimental purposes anyhow. FIXME
	public void testClientFilter() {

		/*
		 * These are credentials the client developer has obtained from the
		 * resource owner. Maybe during the client registration process.
		 */
		String clientId = "client0005";
		String clientPwd = "6h8Hgg$#jak";
		Algorithm algorithm = Algorithm.SHA_256;

		
		/*
		 * Credentials and HawkClientProvider need to be given to filter
		 * when constructed. We use a simple, illustrative version here.
		 * These could well be backed by a database in a real application.
		 */
		final SimpleHawkCredentials credentials = new SimpleHawkCredentials(
				clientId, clientPwd, algorithm);
		SimpleHawkClientProvider provider = new SimpleHawkClientProvider(credentials);

		HawkClientFilter hawkFilter = new HawkClientFilter(provider);
		hawkFilter.setIsHashRequestPayload(false);
		hawkFilter.setIsValidateResponsePayload(false);
		
		/*
		 * Prepare client and HTTP request, configure Hawk filter.
		 */
		Client client = ClientBuilder.newClient();
		WebTarget wt = client.target("http://www.example.org/api/context/news");
		wt.register(new ClientLoggingFilter("Sample client"));
		wt.register(hawkFilter);

		Response response = null;
		try {
			response = wt.request("*/*").get();
		} catch (HawkClockSkewTooLargeException e) {
			long offset;
			long clientNow = System.currentTimeMillis() / 1000L;
			long serverNow = e.getSuggestedTimestamp();
			provider.setClockOffset(serverNow - clientNow);
			// From here, retry request with corrected timestamp.
			// ...
		}
		if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
			// Handle other 401 responses
			return;
		}


	}

}
