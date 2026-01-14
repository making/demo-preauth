package com.example.demo.auth;

import com.example.demo.DemoAppProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Client for communicating with the Auth System.
 * <p>
 * Uses {@link ObjectProvider} for deferred resolution of {@link DemoAppProperties} to
 * ensure dynamic property values from
 * {@link org.springframework.test.context.DynamicPropertyRegistrar} are properly bound
 * during tests.
 */
@Service
public class AuthSystemClient {

	private static final String API_KEY_HEADER = "X-API-Key";

	private final RestClient.Builder restClientBuilder;

	private final ObjectProvider<DemoAppProperties> propertiesProvider;

	private volatile RestClient restClient;

	public AuthSystemClient(RestClient.Builder builder, ObjectProvider<DemoAppProperties> propertiesProvider) {
		this.restClientBuilder = builder;
		this.propertiesProvider = propertiesProvider;
	}

	/**
	 * Validates a token with the auth system.
	 * @param token the token to validate
	 * @return the validation response
	 */
	public ValidateResponse validateToken(String token) {
		return getRestClient().get()
			.uri("/api/validate?token={token}", token)
			.header(API_KEY_HEADER, this.propertiesProvider.getObject().authSystemApiKey())
			.retrieve()
			.body(ValidateResponse.class);
	}

	/**
	 * Returns the RestClient instance, creating it lazily on first access.
	 * <p>
	 * Lazy initialization is required because {@link DemoAppProperties} must be resolved
	 * after {@link org.springframework.test.context.DynamicPropertyRegistrar} has
	 * registered dynamic property values during tests.
	 * @return the RestClient configured with auth-system base URL
	 */
	private RestClient getRestClient() {
		RestClient client = this.restClient;
		if (client == null) {
			synchronized (this) {
				client = this.restClient;
				if (client == null) {
					client = this.restClientBuilder.baseUrl(this.propertiesProvider.getObject().authSystemUrl())
						.build();
					this.restClient = client;
				}
			}
		}
		return client;
	}

}
