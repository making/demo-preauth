package com.example.demo.auth;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Loads user details by validating a pre-authenticated token with the auth system.
 */
@Service
public class TokenUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

	private final AuthSystemClient authSystemClient;

	public TokenUserDetailsService(AuthSystemClient authSystemClient) {
		this.authSystemClient = authSystemClient;
	}

	@Override
	public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
		String tokenValue = (String) token.getPrincipal();

		ValidateResponse response = this.authSystemClient.validateToken(tokenValue);

		if (!response.valid()) {
			throw new UsernameNotFoundException("Token validation failed: " + response.reason());
		}

		return User.builder()
			.username(response.username())
			.password("N/A")
			.authorities(response.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList())
			.build();
	}

}
