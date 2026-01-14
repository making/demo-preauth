package com.example.demo;

import java.time.Duration;

import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	public static final int AUTH_SYSTEM_PORT = 9999;

	public static final int DEMO_APP_PORT = 58080;

	@Bean
	GenericContainer<?> authSystemContainer() {
		return new GenericContainer<>("bellsoft/liberica-openjre-alpine:21")
			.withCopyFileToContainer(MountableFile.forClasspathResource("auth-system-0.0.1-SNAPSHOT.jar"),
					"/auth-system.jar")
			.withCommand("java", "-jar", "/auth-system.jar",
					"--auth.system.allowed-redirect-origins=http://localhost:" + DEMO_APP_PORT + ",http://127.0.0.1:"
							+ DEMO_APP_PORT)
			.withExposedPorts(AUTH_SYSTEM_PORT)
			.waitingFor(Wait.forHttp("/actuator/health")
				.forPort(AUTH_SYSTEM_PORT)
				.forStatusCode(200)
				.withStartupTimeout(Duration.ofSeconds(60)))
			.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("auth-system")));
	}

	@Bean
	DynamicPropertyRegistrar dynamicPropertyRegistrar(GenericContainer<?> authSystemContainer) {
		return registry -> registry.add("demo.app.auth-system-url",
				() -> "http://127.0.0.1:" + authSystemContainer.getMappedPort(AUTH_SYSTEM_PORT));
	}

}
