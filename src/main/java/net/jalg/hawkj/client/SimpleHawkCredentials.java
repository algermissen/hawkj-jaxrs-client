package net.jalg.hawkj.client;

import net.jalg.hawkj.Algorithm;
import net.jalg.hawkj.ext.HawkCredentials;

public class SimpleHawkCredentials implements HawkCredentials {
	
	private String id;
	private String key;
	private Algorithm algorithm;
	
	

	public SimpleHawkCredentials(String id, String key, Algorithm algorithm) {
		super();
		this.id = id;
		this.key = key;
		this.algorithm = algorithm;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getPwd() {
		return this.key;
	}

	@Override
	public Algorithm getAlgorithm() {
		return this.algorithm;
	}

}
