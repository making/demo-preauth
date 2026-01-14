package com.example.demo.config;

import com.example.demo.DemoAppProperties;
import com.example.demo.auth.AuthSystemLogoutSuccessHandler;
import com.example.demo.auth.AuthSystemRedirectEntryPoint;
import com.example.demo.auth.TokenPreAuthenticatedFilter;
import com.example.demo.auth.TokenUserDetailsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

/**
 * Spring Security configuration for the Demo App.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider(
			TokenUserDetailsService tokenUserDetailsService) {
		PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
		provider.setPreAuthenticatedUserDetailsService(tokenUserDetailsService);
		provider.setThrowExceptionWhenTokenRejected(false);
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(PreAuthenticatedAuthenticationProvider preAuthProvider) {
		return new ProviderManager(preAuthProvider);
	}

	@Bean
	TokenPreAuthenticatedFilter tokenPreAuthenticatedFilter(AuthenticationManager authenticationManager) {
		TokenPreAuthenticatedFilter filter = new TokenPreAuthenticatedFilter();
		filter.setAuthenticationManager(authenticationManager);
		filter.setCheckForPrincipalChanges(false);
		// Redirect to clean URL (without token parameter) after successful authentication
		filter.setAuthenticationSuccessHandler(
				(request, response, authentication) -> response.sendRedirect(request.getRequestURI()));
		return filter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, TokenPreAuthenticatedFilter tokenPreAuthenticatedFilter,
			ObjectProvider<DemoAppProperties> propertiesProvider) throws Exception {
		http
		// @formatter:off
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/actuator/health").permitAll()
				.requestMatchers("/admin").hasRole("ADMIN")
				.anyRequest().authenticated())
			// @formatter:on
			.addFilterBefore(tokenPreAuthenticatedFilter, UsernamePasswordAuthenticationFilter.class)
			.exceptionHandling(ex -> ex.authenticationEntryPoint(new AuthSystemRedirectEntryPoint(propertiesProvider)))
			.logout(logout -> logout.logoutSuccessHandler(new AuthSystemLogoutSuccessHandler(propertiesProvider)));
		return http.build();
	}

}
