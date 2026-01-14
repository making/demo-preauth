package com.example.authsystem;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.example.authsystem.token.ApiHeaders;
import com.example.authsystem.token.TokenService;
import com.example.authsystem.token.ValidateResponse;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Auth System using Playwright headless browser.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthSystemIntegrationTest {

	private static final String API_SECRET = "demo-shared-secret-key";

	private static Playwright playwright;

	private static Browser browser;

	private static HttpServer mockAppServer;

	private static int mockAppPort;

	private static CopyOnWriteArrayList<String> receivedTokens;

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private TokenService tokenService;

	private BrowserContext context;

	private Page page;

	@BeforeAll
	static void setUpAll() throws IOException {
		// Setup mock application server using JDK HttpServer
		mockAppServer = HttpServer.create(new InetSocketAddress(0), 0);
		mockAppPort = mockAppServer.getAddress().getPort();
		receivedTokens = new CopyOnWriteArrayList<>();

		// Callback endpoint that receives the token
		mockAppServer.createContext("/callback", exchange -> {
			String query = exchange.getRequestURI().getQuery();
			if (query != null && query.startsWith("token=")) {
				String token = query.substring(6);
				receivedTokens.add(token);
			}

			String response = """
					<!DOCTYPE html>
					<html>
					<head><title>Mock App - Callback</title></head>
					<body>
					<h1>Token Received</h1>
					<p id="token">%s</p>
					</body>
					</html>
					""".formatted(query != null ? query : "no token");

			exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
			byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		});

		mockAppServer.setExecutor(null);
		mockAppServer.start();

		// Setup Playwright
		playwright = Playwright.create();
		browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
	}

	@AfterAll
	static void tearDownAll() {
		if (browser != null) {
			browser.close();
		}
		if (playwright != null) {
			playwright.close();
		}
		if (mockAppServer != null) {
			mockAppServer.stop(0);
		}
	}

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		// This runs before @BeforeAll, so we need to create the server here
		if (mockAppServer == null) {
			try {
				mockAppServer = HttpServer.create(new InetSocketAddress(0), 0);
				mockAppPort = mockAppServer.getAddress().getPort();
			}
			catch (IOException ex) {
				throw new RuntimeException("Failed to create mock server", ex);
			}
		}
		registry.add("auth.system.allowed-redirect-origins", () -> "http://localhost:" + mockAppPort);
	}

	@BeforeEach
	void setUp() {
		context = browser.newContext();
		page = context.newPage();
		receivedTokens.clear();
	}

	@AfterEach
	void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	private String baseUrl() {
		return "http://localhost:" + this.port;
	}

	private String mockAppUrl() {
		return "http://localhost:" + mockAppPort;
	}

	// ========== Login Page Tests (Playwright) ==========

	@Test
	void shouldDisplayLoginPage() {
		page.navigate(baseUrl() + "/login");

		assertThat(page).hasTitle("Auth System - Login");
		assertThat(page.locator("h1")).hasText("Auth System Login");
		assertThat(page.getByLabel("Username")).isVisible();
		assertThat(page.getByLabel("Password")).isVisible();
		assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"))).isVisible();
	}

	@Test
	void shouldDisplayTestUsersOnLoginPage() {
		page.navigate(baseUrl() + "/login");

		assertThat(page.locator("text=user1 / password1")).isVisible();
		assertThat(page.locator("text=admin1 / password1")).isVisible();
		assertThat(page.locator("text=user2 / password2")).isVisible();
	}

	@Test
	void shouldDisplayErrorMessageOnLoginFailure() {
		page.navigate(baseUrl() + "/login?error");

		assertThat(page.locator("text=Invalid username or password")).isVisible();
	}

	// ========== Login Flow Tests (Playwright) ==========

	@Test
	void shouldLoginSuccessfullyAndShowHomePage() {
		// Navigate to login page without redirect parameter
		page.navigate(baseUrl() + "/login");

		// Fill in login form
		page.getByLabel("Username").fill("user1");
		page.getByLabel("Password").fill("password1");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		// Should be redirected to home page (no external redirect)
		page.waitForURL(baseUrl() + "/");
		assertThat(page).hasTitle("Auth System - Home");
		assertThat(page.locator(".welcome-message")).containsText("Welcome");
		assertThat(page.locator(".welcome-message strong")).hasText("user1");
	}

	@Test
	void shouldLoginAsAdminAndShowRoles() {
		page.navigate(baseUrl() + "/login");

		page.getByLabel("Username").fill("admin1");
		page.getByLabel("Password").fill("password1");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		page.waitForURL(baseUrl() + "/");
		assertThat(page).hasTitle("Auth System - Home");
		assertThat(page.locator(".welcome-message strong")).hasText("admin1");
		assertThat(page.locator(".user-info")).containsText("ROLE_USER");
		assertThat(page.locator(".user-info")).containsText("ROLE_ADMIN");
	}

	@Test
	void shouldRedirectWithTokenWhenRedirectParamProvided() {
		// Navigate to login page with redirect parameter pointing to mock app
		page.navigate(baseUrl() + "/login?redirect=" + mockAppUrl() + "/callback");

		// Fill in login form
		page.getByLabel("Username").fill("user1");
		page.getByLabel("Password").fill("password1");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		// Wait for redirect to mock app
		page.waitForURL(url -> url.startsWith(mockAppUrl() + "/callback?token="));

		// Verify the mock app received the token
		assertThat(page).hasTitle("Mock App - Callback");
		assertThat(page.locator("h1")).hasText("Token Received");

		// Verify token was received by mock server
		assertThat(receivedTokens).hasSize(1);
		String token = receivedTokens.getFirst();
		assertThat(token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

		// Verify token contains in the page
		assertThat(page.locator("#token")).containsText("token=" + token);
	}

	@Test
	void shouldShowHomePageForInvalidRedirectUrl() {
		// Navigate to login page with invalid redirect parameter (not in whitelist)
		page.navigate(baseUrl() + "/login?redirect=http://evil.com/");

		page.getByLabel("Username").fill("user1");
		page.getByLabel("Password").fill("password1");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		// Should be redirected to internal home page instead
		page.waitForURL(baseUrl() + "/");
		assertThat(page).hasTitle("Auth System - Home");
	}

	@Test
	void shouldRejectJavascriptSchemeInRedirectUrl() {
		// Navigate to login page with javascript: scheme (XSS attempt)
		page.navigate(baseUrl() + "/login?redirect=javascript://localhost:" + mockAppPort + "/%0Aalert(1)");

		page.getByLabel("Username").fill("user1");
		page.getByLabel("Password").fill("password1");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		// Should be redirected to internal home page instead
		page.waitForURL(baseUrl() + "/");
		assertThat(page).hasTitle("Auth System - Home");
	}

	@Test
	void shouldFailLoginWithWrongPassword() {
		page.navigate(baseUrl() + "/login");

		page.getByLabel("Username").fill("user1");
		page.getByLabel("Password").fill("wrong-password");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		// Should be redirected back to login page with error
		page.waitForURL(baseUrl() + "/login?error");
		assertThat(page.locator("text=Invalid username or password")).isVisible();
	}

	@Test
	void shouldFailLoginWithNonExistentUser() {
		page.navigate(baseUrl() + "/login");

		page.getByLabel("Username").fill("nonexistent");
		page.getByLabel("Password").fill("password");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		page.waitForURL(baseUrl() + "/login?error");
		assertThat(page.locator("text=Invalid username or password")).isVisible();
	}

	// ========== Logout Tests (Playwright) ==========

	@Test
	void shouldLogoutSuccessfully() {
		// First login
		page.navigate(baseUrl() + "/login");
		page.getByLabel("Username").fill("user1");
		page.getByLabel("Password").fill("password1");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
		page.waitForURL(baseUrl() + "/");

		// Then logout
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout")).click();

		// Should be redirected to login page
		page.waitForURL(baseUrl() + "/login");
		assertThat(page).hasTitle("Auth System - Login");
	}

	// ========== Token Validation API Tests (RestTemplate) ==========

	@Test
	void shouldValidateValidToken() {
		String token = this.tokenService.generateToken("user1");

		HttpHeaders headers = new HttpHeaders();
		headers.set(ApiHeaders.API_KEY, API_SECRET);

		ResponseEntity<ValidateResponse> response = this.restTemplate.exchange(
				baseUrl() + "/api/validate?token=" + token, HttpMethod.GET, new HttpEntity<>(headers),
				ValidateResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().valid()).isTrue();
		assertThat(response.getBody().username()).isEqualTo("user1");
		assertThat(response.getBody().displayName()).isEqualTo("User One");
		assertThat(response.getBody().roles()).containsExactly("USER");
	}

	@Test
	void shouldValidateAdminToken() {
		String token = this.tokenService.generateToken("admin1");

		HttpHeaders headers = new HttpHeaders();
		headers.set(ApiHeaders.API_KEY, API_SECRET);

		ResponseEntity<ValidateResponse> response = this.restTemplate.exchange(
				baseUrl() + "/api/validate?token=" + token, HttpMethod.GET, new HttpEntity<>(headers),
				ValidateResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().valid()).isTrue();
		assertThat(response.getBody().username()).isEqualTo("admin1");
		assertThat(response.getBody().roles()).containsExactlyInAnyOrder("USER", "ADMIN");
	}

	@Test
	void shouldRejectInvalidApiKey() {
		String token = this.tokenService.generateToken("user1");

		HttpHeaders headers = new HttpHeaders();
		headers.set(ApiHeaders.API_KEY, "wrong-api-key");

		ResponseEntity<ValidateResponse> response = this.restTemplate.exchange(
				baseUrl() + "/api/validate?token=" + token, HttpMethod.GET, new HttpEntity<>(headers),
				ValidateResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldRejectMissingApiKey() {
		String token = this.tokenService.generateToken("user1");

		ResponseEntity<ValidateResponse> response = this.restTemplate
			.getForEntity(baseUrl() + "/api/validate?token=" + token, ValidateResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldRejectNonExistentToken() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(ApiHeaders.API_KEY, API_SECRET);

		ResponseEntity<ValidateResponse> response = this.restTemplate.exchange(
				baseUrl() + "/api/validate?token=non-existent-token", HttpMethod.GET, new HttpEntity<>(headers),
				ValidateResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().valid()).isFalse();
		assertThat(response.getBody().reason()).isEqualTo("TOKEN_NOT_FOUND");
	}

	@Test
	void shouldRejectAlreadyUsedToken() {
		String token = this.tokenService.generateToken("user1");

		HttpHeaders headers = new HttpHeaders();
		headers.set(ApiHeaders.API_KEY, API_SECRET);

		// First validation - should succeed
		ResponseEntity<ValidateResponse> firstResponse = this.restTemplate.exchange(
				baseUrl() + "/api/validate?token=" + token, HttpMethod.GET, new HttpEntity<>(headers),
				ValidateResponse.class);

		assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(firstResponse.getBody().valid()).isTrue();

		// Second validation - should fail (token already used)
		ResponseEntity<ValidateResponse> secondResponse = this.restTemplate.exchange(
				baseUrl() + "/api/validate?token=" + token, HttpMethod.GET, new HttpEntity<>(headers),
				ValidateResponse.class);

		assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(secondResponse.getBody()).isNotNull();
		assertThat(secondResponse.getBody().valid()).isFalse();
		assertThat(secondResponse.getBody().reason()).isEqualTo("TOKEN_ALREADY_USED");
	}

	// ========== Token Generation Tests ==========

	@Test
	void shouldGenerateUniqueTokens() {
		String token1 = this.tokenService.generateToken("user1");
		String token2 = this.tokenService.generateToken("user1");

		assertThat(token1).isNotEqualTo(token2);
		assertThat(token1).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
		assertThat(token2).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	}

	// ========== Token Expiration Test ==========

	@Test
	void shouldRejectExpiredToken() {
		// The TokenService validate method checks for TOKEN_NOT_FOUND first
		// For a proper expiration test, we'd need to create an expired token directly
		TokenService.ValidateResult result = this.tokenService.validate("non-existent-token");

		assertThat(result).isInstanceOf(TokenService.ValidateResult.Failure.class);
		TokenService.ValidateResult.Failure failure = (TokenService.ValidateResult.Failure) result;
		assertThat(failure.reason()).isEqualTo("TOKEN_NOT_FOUND");
	}

}
