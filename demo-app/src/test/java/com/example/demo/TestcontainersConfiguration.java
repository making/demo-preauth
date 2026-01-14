package com.example.demo;

import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	GenericContainer<?> authorizationServer() {
		return new GenericContainer<>("bellsoft/liberica-openjre-alpine:21")
			.withCopyFileToContainer(MountableFile.forClasspathResource("auth-system-0.0.1-SNAPSHOT.jar"),
					"/auth-system.jar")
			.withCommand("java", "-jar", "/auth-system.jar")
			.withExposedPorts(9999)
			.waitingFor(Wait.forHttp("/actuator/health")
				.forPort(9999)
				.forStatusCode(200)
				.withStartupTimeout(Duration.ofSeconds(10)))
			.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("auth-system")));
	}

	@Bean
	DynamicPropertyRegistrar dynamicPropertyRegistrar(GenericContainer<?> authorizationServer) {
		return registry -> registry.add("demo.app.auth-system-url",
				() -> "http://127.0.0.1:" + authorizationServer.getMappedPort(9000));
	}

}
