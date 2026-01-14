package com.example.demo.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

/**
 * Filter that extracts the authentication token from query parameters. Extends
 * AbstractPreAuthenticatedProcessingFilter to integrate with Spring Security's
 * pre-authentication mechanism.
 */
public class TokenPreAuthenticatedFilter extends AbstractPreAuthenticatedProcessingFilter {

	private static final String TOKEN_PARAMETER = "token";

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		return request.getParameter(TOKEN_PARAMETER);
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		return "N/A";
	}

}
