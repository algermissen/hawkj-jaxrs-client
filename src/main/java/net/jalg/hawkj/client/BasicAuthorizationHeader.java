package net.jalg.hawkj.client;

import javax.xml.bind.DatatypeConverter;

import net.jalg.hawkj.AuthDirectiveBuilder;
import net.jalg.hawkj.AuthDirectiveParser;
import net.jalg.hawkj.AuthHeaderParsingException;

public class BasicAuthorizationHeader  {
	

	private String login;
	private String password;

	public String getLogin() {
		return login;
	}

	public String getPassword() {
		return password;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("Basic ");
		// FIXME use other base64
		String t = login + ":" + password;
        String s = DatatypeConverter.printBase64Binary(t.getBytes());
		sb.append(s);
		return sb.toString();
	}

	private BasicAuthorizationHeader() {
	}

	public static AuthorizeBuilder authorize() {
		return new AuthorizeBuilder();
	}

	public static BasicAuthorizationHeader authorize(String value)
			throws AuthHeaderParsingException {
		AuthorizeBuilder b = new AuthorizeBuilder();
		AuthDirectiveParser p = new AuthDirectiveParser(value, b);
		p.parse();
		return b.build();
	}

	public static class AuthorizeBuilder implements AuthDirectiveBuilder {
		private String login;
		private String password;
		private boolean haveSeenToken;
		

		private AuthorizeBuilder() {
			this.haveSeenToken = false;
		}

		public BasicAuthorizationHeader build() {
			BasicAuthorizationHeader instance = new BasicAuthorizationHeader();
			instance.login = this.login;
			instance.password = this.password;
			return instance;
		}

		public AuthorizeBuilder credentials(String login,String password) {
			this.login = login;
			this.password = password;
			return this;
		}

		@Override
		public void scheme(String scheme) throws AuthHeaderParsingException {
			// check null;
			if (!"basic".equals(scheme.toLowerCase())) {
				throw new AuthHeaderParsingException("FIXME");
			}
		}

		@Override
		public void token(String token)
				throws AuthHeaderParsingException {
			
			if(this.haveSeenToken) {
				throw new AuthHeaderParsingException("can only handle on token FIXME");
			}
			this.haveSeenToken = true;
			
			byte[] decodedBytes = DatatypeConverter.parseBase64Binary(token);

	        //If the decode fails in any case
	        if(decodedBytes == null || decodedBytes.length == 0){
				throw new AuthHeaderParsingException("Error Basic-auth splitting token" + token);
	        }

	        //Now we can convert the byte[] into a splitted array :
	        //  - the first one is login,
	        //  - the second one password
			String[] credentials =  new String(decodedBytes).split(":", 2);
			
	 
	        if(credentials == null || credentials.length != 2){
	            throw new AuthHeaderParsingException("FIXME");
	        }
	        
			this.login = credentials[0];
			this.password = credentials[1];
	 
			
		}

		@Override
		public void param(String key, String value)
				throws AuthHeaderParsingException {
			// TODO Auto-generated method stub
			
		}

	}


}
