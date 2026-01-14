package com.example.authsystem.config;

import java.time.InstantSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application configuration for common beans.
 */
@Configuration(proxyBeanMethods = false)
public class AppConfig {

	@Bean
	InstantSource instantSource() {
		return InstantSource.system();
	}

}
