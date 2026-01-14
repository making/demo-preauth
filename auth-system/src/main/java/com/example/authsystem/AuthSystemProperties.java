package com.example.authsystem;

import java.time.Duration;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for the Auth System.
 */
@ConfigurationProperties(prefix = "auth.system")
public record AuthSystemProperties(@DefaultValue("demo-shared-secret-key") String apiSecret,
		@DefaultValue("5m") Duration tokenExpiry,
		@DefaultValue("http://localhost:8080") Set<String> allowedRedirectOrigins) {
}
