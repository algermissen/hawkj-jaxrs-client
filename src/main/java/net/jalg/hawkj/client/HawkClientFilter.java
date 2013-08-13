package net.jalg.hawkj.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import net.jalg.hawkj.AuthHeaderParsingException;
import net.jalg.hawkj.AuthorizationHeader;
import net.jalg.hawkj.HawkContext;
import net.jalg.hawkj.HawkContext.HawkContextBuilder;
import net.jalg.hawkj.HawkContext.HawkContextBuilder_B;
import net.jalg.hawkj.ext.HawkCredentials;
import net.jalg.hawkj.ext.InputStreamBuffer;
import net.jalg.hawkj.HawkWwwAuthenticateContext;
import net.jalg.hawkj.Util;
import net.jalg.hawkj.WwwAuthenticateHeader;

/**
 * Client side filter and interceptor for adding and validating Hawk HTTP
 * Authentication.
 * 
 * FIXME: describe the overall chain handling.
 * 
 * @author Jan Algermissen, http://jalg.net
 * 
 */
public class HawkClientFilter implements ClientRequestFilter,
		ClientResponseFilter, WriterInterceptor, ReaderInterceptor {

	private static final String HAWK_CLIENT_PROPERTY = "net.jalg.ono.client.jhawk.hawk";
	private static final String HAWK_BUILDER_CLIENT_PROPERTY = "net.jalg.ono.client.jhawk.builder";

	private static Logger LOG = Logger.getLogger(HawkClientFilter.class
			.getName());

	private final HawkClientProvider hawkProvider;
	private final boolean hashRequestPayload;
	private final boolean validateResponsePayload;

	/**
	 * Create a new instance of this filter and interceptor.
	 * 
	 * @param hawkProvider
	 * @param hashRequestPayload
	 * @param validateResponsePayload
	 */
	public HawkClientFilter(HawkClientProvider hawkProvider,
			boolean hashRequestPayload, boolean validateResponsePayload) {
		this.hawkProvider = hawkProvider;
		this.hashRequestPayload = hashRequestPayload;
		this.validateResponsePayload = validateResponsePayload;
	}

	@Override
	public void filter(final ClientRequestContext requestContext)
			throws IOException {
		if (requestContext.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
			throw new ProcessingException(
					"There is already an Authorization header present in this request.");
		}
		/*
		 * Create a HawkBuilder to be used either directly below (in case we do
		 * not want payload hashing or there is no payload) or in the
		 * WriterInterceptor after calculating the body hash.
		 */
		URI uri = requestContext.getUri();
		
		HawkCredentials credentials = hawkProvider.getCredentials(uri);
		
		HawkContextBuilder_B requestHawkBuilder = HawkContext.request(
				requestContext.getMethod(), uri.getPath(), uri.getHost(),
				uri.getPort()).credentials(
				credentials.getId(),
				credentials.getPwd(),
				credentials.getAlgorithm());
		/*
		 * Add Authorization header now. The request Hawk created to do this we
		 * save in the context to be used during response handling. It enables
		 * the ReaderInterceptor to get hold of request method, URI etc.
		 * 
		 * We cannot do this in the writer interceptor in all cases because if
		 * we do not have a request entity, the interceptor will not be called.
		 */
		if (!hashRequestPayload || !requestContext.hasEntity()) {
			HawkContext requestHawk = requestHawkBuilder.build();
			AuthorizationHeader authHeader = requestHawk
					.createAuthorizationHeader();
			requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION,
					authHeader.toString());
			// Remember request Hawk for handling Server-Authorization header
			requestContext.setProperty(HAWK_CLIENT_PROPERTY, requestHawk);
			return;
		}

		/*
		 * Defer Authorization header creation to WriterInterceptor.
		 */
		requestContext.setProperty(HAWK_BUILDER_CLIENT_PROPERTY,
				requestHawkBuilder);
	}

	@Override
	public void aroundWriteTo(WriterInterceptorContext context)
			throws IOException, WebApplicationException {

		HawkContextBuilder_B requestHawkBuilder = (HawkContextBuilder_B) context
				.getProperty(HAWK_BUILDER_CLIENT_PROPERTY);
		/*
		 * If there is no HawkBuilder in the context, there is nothing for us to
		 * do.
		 */
		if (requestHawkBuilder == null) {
			context.proceed();
			return;
		}
		/*
		 * Could be that some other filters added an Authorization header before
		 * this writer interceptor method is called.
		 */
		if (context.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
			throw new ProcessingException(
					"There is already an Authorization header present in this request.");
		}

		/*
		 * Read complete payload from stream into a buffer, calculate hash and
		 * add that to the request Hawk.
		 */
		OutputStream old = context.getOutputStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		context.setOutputStream(baos);
		context.proceed();
		final byte[] body = baos.toByteArray();

		HawkContext requestHawk = requestHawkBuilder.body(body,
				context.getMediaType().toString()).build();
		context.getHeaders().add(HttpHeaders.AUTHORIZATION,
				requestHawk.createAuthorizationHeader().toString());
		old.write(body);
		/*
		 * The request Hawk created above we save in the context to be used
		 * during response handling. It enables the ReaderInterceptor to get
		 * hold of request method, URI etc.
		 */
		context.setProperty(HAWK_CLIENT_PROPERTY, requestHawk);
	}

	@Override
	public void filter(ClientRequestContext requestContext,
			ClientResponseContext responseContext) throws IOException {

		/*
		 * Since Location header is not part of the signature Hawk defines, we
		 * discourage here, to follow redirects and 'block' processing of 3xx
		 * and any response that contains 'Location' header.
		 */
		if (responseContext.getStatusInfo().getFamily()
				.equals(Status.Family.INFORMATIONAL)) {
			// Should Hawk include Location header in
			throw new HawkSecurityException(
					"For security reasons informational responses are discouraged right now");
		}

		if (responseContext.getHeaders().containsKey(HttpHeaders.LOCATION)) {
			throw new HawkSecurityException(
					"For security reasons responses with Location headers are discouraged right now");
		}

		if (responseContext.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
			if (!responseContext.getHeaders().containsKey(
					HttpHeaders.WWW_AUTHENTICATE)) {
				throw new ProcessingException(
						"Missing WWW-Authenticate header on 401 response");
			}
			/*
			 * Parse the Server-Authorization header. FIXME: need to support
			 * multiple WWW-Authenticate headers and then pick the first with
			 * Hawk scheme. This relates to header parser issue
			 * https://github.com/algermissen/hawkj/issues/3
			 */
			WwwAuthenticateHeader wwwAuthenticateHeader;
			try {
				wwwAuthenticateHeader = WwwAuthenticateHeader
						.wwwAuthenticate(responseContext.getHeaders().getFirst(
								HttpHeaders.WWW_AUTHENTICATE));
			} catch (AuthHeaderParsingException e) {
				throw new HawkSecurityException(
						"Unable to parse WWW-Authenticate header: "
								+ responseContext.getHeaders().getFirst(
										HttpHeaders.WWW_AUTHENTICATE), e);
			}

			/*
			 * If the 401 response is due to a clock skew that is too big, the
			 * server might have included a 'ts' parameter in the response. If
			 * so, we need to make sure the timestamp signature is valid, before
			 * proceeding in the filter chain.
			 */
			if (wwwAuthenticateHeader.hasTs()) {
				
				/*
				 * Obtain request Hawk from context for building response Hawk (the
				 * request Hawk gives us access to the request data and credentials used
				 * in the request.
				 */
				HawkContext requestHawk = (HawkContext) requestContext
						.getProperty(HAWK_CLIENT_PROPERTY);
				if (requestHawk == null) {
					throw new ProcessingException(
							"Request hawk context not present in properties");
				}
				
				HawkWwwAuthenticateContext c = HawkWwwAuthenticateContext
						.tsAndTsm(wwwAuthenticateHeader.getTs(),
								wwwAuthenticateHeader.getTsm())
						.credentials(requestHawk.getId(),
								requestHawk.getKey(),
								requestHawk.getAlgorithm())
						.build();
				/*
				 * Validate the timestamp HMAC to make sure that timestamp has
				 * not been tempered with.
				 */
				if (!c.isValidTimestampMac(wwwAuthenticateHeader.getTsm())) {
					throw new HawkSecurityException(
							"Unable to validate HMAC signature of timestamp, header: "
									+ wwwAuthenticateHeader.toString());
				}

				/*
				 * Actually *using* the timestamp is up to the client impl.
				 * Coming here merely means that the filter has validated the
				 * response so far.
				 */
			}
		}

	}

	@Override
	public Object aroundReadFrom(ReaderInterceptorContext context)
			throws IOException, WebApplicationException {
		if (!this.validateResponsePayload) {
			return context.proceed();
		}
		if (!context.getHeaders().containsKey(HawkContext.SERVER_AUTHORIZATION)) {
			throw new HawkSecurityException(
					HawkClientFilter.class.getName()
							+ " instance is configured to validate response payload, but no Server-Authorization header in response.");
		}

		/*
		 * Parse the Server-Authorization header.
		 */
		AuthorizationHeader serverAuthHeader;
		try {
			serverAuthHeader = AuthorizationHeader.authorization(context
					.getHeaders().getFirst(HawkContext.SERVER_AUTHORIZATION));
		} catch (AuthHeaderParsingException e) {
			throw new HawkSecurityException(
					"Unable to parse HTTP Server-Authorization header: "
							+ context.getHeaders().getFirst(
									HawkContext.SERVER_AUTHORIZATION), e);
		}

		/*
		 * Obtain request Hawk from context for building response Hawk (the
		 * request Hawk gives us access to the request data and credentials used
		 * in the request.
		 */
		HawkContext requestHawk = (HawkContext) context
				.getProperty(HAWK_CLIENT_PROPERTY);
		if (requestHawk == null) {
			throw new ProcessingException(
					"Request hawk context not present in properties");
		}

		/*
		 * Make sure that the Server-Authorization header we received matches
		 * the request Hawk we have stored. To do this we check id, ts and
		 * nonce, which per spec need to be the same. If these match, we can use
		 * the other data (e.g. the key) from the request Hawk to build response
		 * Hawk.
		 */
		if (!requestHawk.verifyServerAuthorizationMatches(serverAuthHeader)) {
			throw new HawkSecurityException(
					"Server-Authorization header does not match request hawk context, header:"
							+ serverAuthHeader.toString());
		}

		/*
		 * Now build response Hawk to get all the parameters we need to validate
		 * the HMAC and payload hash in Server-Authorization header.
		 * 
		 * FIXME: is hash mandatory? what happens on null?
		 */
		HawkContext responseHawk = requestHawk.cloneC()
				.hash(serverAuthHeader.getHash()).build();

		if (!responseHawk.isValidMac(serverAuthHeader.getMac())) {
			throw new HawkSecurityException(
					"Unable to validate HMAC signature of Server-Authorization, header: "
							+ serverAuthHeader.toString());
		}

		/*
		 * Hook stream buffer input stream into the input stream. This will
		 * cause the read bytes to be copied into a buffer. From this buffer we
		 * create the hash when reading is done. Then we return the entity
		 * object.
		 */
		InputStream old = context.getInputStream();
		InputStreamBuffer stream = new InputStreamBuffer(old);
		context.setInputStream(stream);
		Object entity = context.proceed();
		byte[] body = stream.getBuffer();

		String hash = HawkContextBuilder.generateHash(requestHawk
				.getAlgorithm(), body, context.getMediaType().toString());
		if (!Util.fixedTimeEqual(hash, responseHawk.getHash())) {
			throw new HawkSecurityException(
					"Response payload hash not valid. ResponseHawk: "
							+ responseHawk.toString() + ", hash: " + hash);
		}
		return entity;

	}

}
