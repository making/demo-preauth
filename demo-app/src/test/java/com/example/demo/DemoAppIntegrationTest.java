package com.example.demo;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Integration tests for the Demo App using Playwright headless browser.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
		properties = "server.port=" + TestcontainersConfiguration.DEMO_APP_PORT)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class DemoAppIntegrationTest {

	private static Playwright playwright;

	private static Browser browser;

	private BrowserContext context;

	private Page page;

	@BeforeAll
	static void setUpAll() {
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
	}

	@BeforeEach
	void setUp() {
		this.context = browser.newContext();
		this.page = this.context.newPage();
	}

	@AfterEach
	void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	private String baseUrl() {
		return "http://localhost:" + TestcontainersConfiguration.DEMO_APP_PORT;
	}

	// ========== Authentication Flow Tests ==========

	@Test
	void shouldRedirectToAuthSystemWhenNotAuthenticated() {
		this.page.navigate(baseUrl() + "/");

		// Should be redirected to auth-system login page
		assertThat(this.page).hasTitle("Auth System - Login");
		assertThat(this.page.locator("h1")).hasText("Login");
	}

	@Test
	void shouldLoginAndShowHomePage() {
		this.page.navigate(baseUrl() + "/");

		// Should be redirected to auth-system login page
		assertThat(this.page).hasTitle("Auth System - Login");

		// Fill in login form
		this.page.getByLabel("Username").fill("user1");
		this.page.getByLabel("Password").fill("password1");
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		// Should be redirected back to demo-app home page
		this.page.waitForURL(url -> url.startsWith(baseUrl() + "/"));
		assertThat(this.page).hasTitle("Demo App - Home");
		assertThat(this.page.locator("h1")).hasText("Home");
	}

	@Test
	void shouldShowCorrectUserInfoOnHomePage() {
		this.page.navigate(baseUrl() + "/");

		// Login
		this.page.getByLabel("Username").fill("user1");
		this.page.getByLabel("Password").fill("password1");
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		this.page.waitForURL(url -> url.startsWith(baseUrl() + "/"));

		// Verify user info is displayed correctly
		assertThat(this.page.locator(".message strong")).hasText("user1");
		assertThat(this.page.locator(".user-info")).containsText("user1");
		assertThat(this.page.locator(".user-info")).containsText("ROLE_USER");
	}

	@Test
	void shouldShowAdminRolesForAdminUser() {
		this.page.navigate(baseUrl() + "/");

		// Login as admin
		this.page.getByLabel("Username").fill("admin1");
		this.page.getByLabel("Password").fill("password1");
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		this.page.waitForURL(url -> url.startsWith(baseUrl() + "/"));

		// Verify admin roles are displayed
		assertThat(this.page.locator(".message strong")).hasText("admin1");
		assertThat(this.page.locator(".user-info")).containsText("ROLE_USER");
		assertThat(this.page.locator(".user-info")).containsText("ROLE_ADMIN");
	}

	// ========== Dashboard Access Tests ==========

	@Test
	void shouldAccessDashboardAsUser() {
		this.page.navigate(baseUrl() + "/");

		// Login
		this.page.getByLabel("Username").fill("user1");
		this.page.getByLabel("Password").fill("password1");
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		this.page.waitForURL(url -> url.startsWith(baseUrl() + "/"));

		// Click dashboard link
		this.page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Dashboard")).click();

		// Verify dashboard page is displayed
		this.page.waitForURL(baseUrl() + "/dashboard");
		assertThat(this.page).hasTitle("Demo App - Dashboard");
		assertThat(this.page.locator("h1")).hasText("Dashboard");
		assertThat(this.page.locator(".message strong")).hasText("user1");
	}

	// ========== Admin Page Access Tests ==========

	@Test
	void shouldAccessAdminPageAsAdmin() {
		this.page.navigate(baseUrl() + "/");

		// Login as admin
		this.page.getByLabel("Username").fill("admin1");
		this.page.getByLabel("Password").fill("password1");
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		this.page.waitForURL(url -> url.startsWith(baseUrl() + "/"));

		// Click admin page link
		this.page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Admin Page")).click();

		// Verify admin page is displayed
		this.page.waitForURL(baseUrl() + "/admin");
		assertThat(this.page).hasTitle("Demo App - Admin");
		assertThat(this.page.locator("h1")).hasText("Admin");
		assertThat(this.page.locator(".message strong")).hasText("admin1");
	}

	@Test
	void shouldDenyAdminPageForNonAdminUser() {
		this.page.navigate(baseUrl() + "/");

		// Login as regular user
		this.page.getByLabel("Username").fill("user1");
		this.page.getByLabel("Password").fill("password1");
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		this.page.waitForURL(url -> url.startsWith(baseUrl() + "/"));

		// Verify admin link is not visible for regular user
		assertThat(this.page.locator("a.admin-link")).not().isVisible();

		// Try to access admin page directly
		this.page.navigate(baseUrl() + "/admin");

		// Should get 403 Forbidden error
		assertThat(this.page.locator("body")).containsText("403");
	}

	// ========== Logout Tests ==========

	@Test
	void shouldLogoutAndRedirectToAuthSystem() {
		this.page.navigate(baseUrl() + "/");

		// Login
		this.page.getByLabel("Username").fill("user1");
		this.page.getByLabel("Password").fill("password1");
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		this.page.waitForURL(url -> url.startsWith(baseUrl() + "/"));

		// Logout
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout")).click();

		// Should be redirected to auth-system login page
		assertThat(this.page).hasTitle("Auth System - Login");
	}

	@Test
	void shouldRequireLoginAfterLogout() {
		this.page.navigate(baseUrl() + "/");

		// Login
		this.page.getByLabel("Username").fill("user1");
		this.page.getByLabel("Password").fill("password1");
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		this.page.waitForURL(url -> url.startsWith(baseUrl() + "/"));

		// Logout
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout")).click();

		// Try to access protected page
		this.page.navigate(baseUrl() + "/dashboard");

		// Should be redirected to auth-system login page
		assertThat(this.page).hasTitle("Auth System - Login");
	}

	// ========== Invalid Token Tests ==========

	@Test
	void shouldRejectInvalidToken() {
		// Try to access with invalid token
		this.page.navigate(baseUrl() + "/?token=invalid-token-12345");

		// Should be redirected to auth-system login page
		assertThat(this.page).hasTitle("Auth System - Login");
	}

	// ========== Session Persistence Tests ==========

	@Test
	void shouldMaintainSessionAfterAuthentication() {
		this.page.navigate(baseUrl() + "/");

		// Login
		this.page.getByLabel("Username").fill("user1");
		this.page.getByLabel("Password").fill("password1");
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		this.page.waitForURL(url -> url.startsWith(baseUrl() + "/"));

		// Navigate to dashboard
		this.page.navigate(baseUrl() + "/dashboard");
		assertThat(this.page).hasTitle("Demo App - Dashboard");

		// Navigate back to home
		this.page.navigate(baseUrl() + "/");
		assertThat(this.page).hasTitle("Demo App - Home");

		// User should still be authenticated
		assertThat(this.page.locator(".message strong")).hasText("user1");
	}

	// ========== Navigation Tests ==========

	@Test
	void shouldNavigateBackToHomeFromDashboard() {
		this.page.navigate(baseUrl() + "/");

		// Login
		this.page.getByLabel("Username").fill("user1");
		this.page.getByLabel("Password").fill("password1");
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		this.page.waitForURL(url -> url.startsWith(baseUrl() + "/"));

		// Navigate to dashboard
		this.page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Dashboard")).click();
		this.page.waitForURL(baseUrl() + "/dashboard");

		// Navigate back to home
		this.page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Back to Home")).click();

		this.page.waitForURL(baseUrl() + "/");
		assertThat(this.page).hasTitle("Demo App - Home");
	}

	@Test
	void shouldNavigateBackToHomeFromAdmin() {
		this.page.navigate(baseUrl() + "/");

		// Login as admin
		this.page.getByLabel("Username").fill("admin1");
		this.page.getByLabel("Password").fill("password1");
		this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

		this.page.waitForURL(url -> url.startsWith(baseUrl() + "/"));

		// Navigate to admin page
		this.page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Admin Page")).click();
		this.page.waitForURL(baseUrl() + "/admin");

		// Navigate back to home
		this.page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Back to Home")).click();

		this.page.waitForURL(baseUrl() + "/");
		assertThat(this.page).hasTitle("Demo App - Home");
	}

}
