package net.jalg.hawkj.client;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Provider
public class ClientLoggingFilter implements ClientRequestFilter, ClientResponseFilter {

	private static Logger log = Logger.getLogger(ClientLoggingFilter.class.getName());
	private String name;

    public ClientLoggingFilter(String name) {
    	this.name = name;
    }

	@Override
	public void filter(ClientRequestContext req) throws IOException {
		log.log(Level.INFO, name + "- Client Request -----------------------------");
		log.log(Level.INFO, name + "> {0} {1}", new Object[] {req.getMethod(),req.getUri().toASCIIString()});
		MultivaluedMap<String, Object> headers = req.getHeaders();
		for (String key : headers.keySet()) {
			List<Object> values = headers.get(key);
			log.log(Level.INFO, name + "> {0}: {1}", new Object[] {key,values});
		}
		log.log(Level.INFO, name + "------------------------------");
	}
	
	@Override
	public void filter(ClientRequestContext req, ClientResponseContext res)
			throws IOException {
		log.log(Level.INFO, name + "- Client Response -----------------------------");
		log.log(Level.INFO, name + "< {0}",res.getStatus());
		MultivaluedMap<String, String> headers = res.getHeaders();
		for (String key : headers.keySet()) {
			List<String> values = headers.get(key);
			log.log(Level.INFO, name + "< {0}: {1}", new Object[] {key,values});
		}
		log.log(Level.INFO, name + "-------------------------------");

	}
}

