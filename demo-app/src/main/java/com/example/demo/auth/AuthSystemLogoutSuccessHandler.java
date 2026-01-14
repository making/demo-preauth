package com.example.demo.auth;

import java.io.IOException;

import com.example.demo.DemoAppProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * LogoutSuccessHandler that redirects users to the auth-system's login page after logout.
 * <p>
 * Uses {@link ObjectProvider} for deferred resolution of {@link DemoAppProperties} to
 * ensure dynamic property values from
 * {@link org.springframework.test.context.DynamicPropertyRegistrar} are properly bound
 * during tests.
 */
public class AuthSystemLogoutSuccessHandler implements LogoutSuccessHandler {

	private final ObjectProvider<DemoAppProperties> propertiesProvider;

	public AuthSystemLogoutSuccessHandler(ObjectProvider<DemoAppProperties> propertiesProvider) {
		this.propertiesProvider = propertiesProvider;
	}

	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException, ServletException {
		String applicationBaseUrl = ServletUriComponentsBuilder.fromContextPath(request).path("/").toUriString();
		String redirectUrl = UriComponentsBuilder.fromUriString(this.propertiesProvider.getObject().authSystemUrl())
			.path("/login")
			.queryParam("redirect", applicationBaseUrl)
			.toUriString();
		response.sendRedirect(redirectUrl);
	}

}
