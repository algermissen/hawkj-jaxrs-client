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

	String clientId = "abc";
	String clientPwd = "cde";
	Algorithm algorithm = Algorithm.SHA_256;

	final static String URI = "http://localhost:8080/product/api/test.txt";

	@Test
	public void testIt() {

		final int clockOffset = 0;
		Client client = ClientBuilder.newClient();
		final SimpleHawkCredentials clientCredentials = new SimpleHawkCredentials(
				clientId, clientPwd, Algorithm.SHA_256);

		WebTarget wt = client.target(URI);
		/*
		 * wt.register(DemandMessageBodyWriter.class);
		 * wt.register(OnoMessageBodyReader.class);
		 */
		HawkClientFilter f1 = new HawkClientFilter(new HawkClientProvider() {

			public HawkCredentials getCredentials(URI uri) {
				return clientCredentials;
			}

			@Override
			public int getClockOffset() {
				return clockOffset;
			}

		}, true, true);

		wt.register(new ClientLoggingFilter("Trusted Client"));
		wt.register(f1);

		Response response = null;
		response = wt.request("application/ono").get();
		if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
			//int ts = handle401TsResponse(response);
			System.out.println("Suggested TS: " + ts);
			return;
		}

		System.out.println("***------------------- " + response.getStatus());

	}

}
