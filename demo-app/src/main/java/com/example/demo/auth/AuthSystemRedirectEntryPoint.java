package com.example.demo.auth;

import java.io.IOException;

import com.example.demo.DemoAppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Custom AuthenticationEntryPoint that redirects unauthenticated users to the auth
 * system's login page.
 * <p>
 * Uses {@link ObjectProvider} for deferred resolution of {@link DemoAppProperties} to
 * ensure dynamic property values from
 * {@link org.springframework.test.context.DynamicPropertyRegistrar} are properly bound
 * during tests.
 */
public class AuthSystemRedirectEntryPoint implements AuthenticationEntryPoint {

	private final ObjectProvider<DemoAppProperties> propertiesProvider;

	public AuthSystemRedirectEntryPoint(ObjectProvider<DemoAppProperties> propertiesProvider) {
		this.propertiesProvider = propertiesProvider;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {

		String applicationUrl = ServletUriComponentsBuilder.fromRequestUri(request).toUriString();

		String redirectUrl = UriComponentsBuilder.fromUriString(this.propertiesProvider.getObject().authSystemUrl())
			.path("/login")
			.queryParam("redirect", applicationUrl)
			.toUriString();

		response.sendRedirect(redirectUrl);
	}

}
