package com.example.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for Demo App.
 */
@ConfigurationProperties(prefix = "demo.app")
public record DemoAppProperties(String authSystemUrl, String authSystemApiKey) {

}
