package com.example.authsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the Auth System.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
		// @formatter:off
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/login", "/api/validate", "/actuator/health", "/*.css").permitAll()
				.anyRequest().authenticated())
			// @formatter:on
			.formLogin(form -> form.loginPage("/login")
				.loginProcessingUrl("/login")
				.defaultSuccessUrl("/login-success", true)
				.failureUrl("/login?error")
				.permitAll())
			.logout(logout -> logout.logoutSuccessUrl("/login").permitAll())
			.csrf(csrf -> csrf.ignoringRequestMatchers("/api/validate"));

		return http.build();
	}

}
