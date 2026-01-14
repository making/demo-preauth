package com.example.authsystem.token.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authsystem.AuthSystemProperties;
import com.example.authsystem.auth.UserInfo;
import com.example.authsystem.token.ApiHeaders;
import com.example.authsystem.token.TokenService;
import com.example.authsystem.token.TokenService.ValidateResult;
import com.example.authsystem.token.ValidateResponse;

/**
 * REST controller for token validation API.
 */
@RestController
@RequestMapping("/api")
public class ValidateController {

	private final TokenService tokenService;

	private final AuthSystemProperties properties;

	public ValidateController(TokenService tokenService, AuthSystemProperties properties) {
		this.tokenService = tokenService;
		this.properties = properties;
	}

	/**
	 * Validates a token and returns user information if valid.
	 * @param apiKey the API key from the X-API-Key header
	 * @param token the token to validate
	 * @return the validation response
	 */
	@GetMapping("/validate")
	public ResponseEntity<ValidateResponse> validate(
			@RequestHeader(name = ApiHeaders.API_KEY, required = false) String apiKey, @RequestParam String token) {

		// Validate API key
		if (apiKey == null || !this.properties.apiSecret().equals(apiKey)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// Validate token
		ValidateResult result = this.tokenService.validate(token);

		return switch (result) {
			case ValidateResult.Success(UserInfo userInfo) -> ResponseEntity.ok(ValidateResponse.success(userInfo));
			case ValidateResult.Failure(String reason) -> ResponseEntity.ok(ValidateResponse.failure(reason));
		};
	}

}
