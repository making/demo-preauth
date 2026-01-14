package com.example.authsystem.auth.web;

import java.net.URI;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.authsystem.AuthSystemProperties;
import com.example.authsystem.token.TokenService;

import jakarta.servlet.http.HttpSession;

/**
 * Controller for login page and login success handling.
 */
@Controller
public class LoginController {

	private static final String REDIRECT_URL_SESSION_KEY = "redirectUrl";

	private final TokenService tokenService;

	private final AuthSystemProperties properties;

	public LoginController(TokenService tokenService, AuthSystemProperties properties) {
		this.tokenService = tokenService;
		this.properties = properties;
	}

	/**
	 * Displays the login page.
	 * @param redirect the URL to redirect to after successful login
	 * @param error indicates if there was a login error
	 * @param session the HTTP session
	 * @param model the model for the view
	 * @return the login view name
	 */
	@GetMapping("/login")
	public String loginPage(@RequestParam(required = false) String redirect,
			@RequestParam(required = false) String error, HttpSession session, Model model) {

		if (redirect != null && !redirect.isBlank()) {
			session.setAttribute(REDIRECT_URL_SESSION_KEY, redirect);
		}

		if (error != null) {
			model.addAttribute("error", true);
		}

		return "login";
	}

	/**
	 * Handles successful login by generating a token and redirecting to the application,
	 * or showing the default top page if no redirect URL was provided.
	 * @param session the HTTP session
	 * @param authentication the authentication object
	 * @param redirectAttributes the redirect attributes
	 * @return the redirect URL with token or internal top page
	 */
	@GetMapping("/login-success")
	public String loginSuccess(HttpSession session, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		String redirectUrl = (String) session.getAttribute(REDIRECT_URL_SESSION_KEY);
		session.removeAttribute(REDIRECT_URL_SESSION_KEY);

		// If no redirect URL provided, show internal top page
		if (redirectUrl == null || redirectUrl.isBlank()) {
			return "redirect:/";
		}

		// Validate redirect URL
		if (!isValidRedirectUrl(redirectUrl)) {
			// Invalid redirect URL, show internal top page instead
			return "redirect:/";
		}

		// Generate token
		String token = this.tokenService.generateToken(authentication.getName());

		// Add token as query parameter
		redirectAttributes.addAttribute("token", token);
		return "redirect:" + redirectUrl;
	}

	/**
	 * Displays the default top page for authenticated users.
	 * @param authentication the authentication object
	 * @param model the model for the view
	 * @return the home view name
	 */
	@GetMapping("/")
	public String home(Authentication authentication, Model model) {
		model.addAttribute("username", authentication.getName());
		model.addAttribute("roles", authentication.getAuthorities());
		return "home";
	}

	private boolean isValidRedirectUrl(String redirectUrl) {
		try {
			URI uri = URI.create(redirectUrl);

			// Validate scheme is http or https only
			String scheme = uri.getScheme();
			if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
				return false;
			}

			// Validate host is present
			String host = uri.getHost();
			if (host == null || host.isBlank()) {
				return false;
			}

			// Build origin (scheme://host:port)
			int port = uri.getPort();
			String origin = (port > 0) ? scheme + "://" + host + ":" + port : scheme + "://" + host;

			return this.properties.allowedRedirectOrigins().contains(origin);
		}
		catch (Exception ex) {
			return false;
		}
	}

}
