package com.example.authsystem.token;

import java.time.Instant;
import java.time.InstantSource;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.authsystem.auth.UserInfo;
import com.example.authsystem.auth.UserService;
import com.example.authsystem.AuthSystemProperties;

/**
 * Service for generating and validating authentication tokens.
 */
@Service
public class TokenService {

	private final ConcurrentHashMap<String, TokenInfo> tokens = new ConcurrentHashMap<>();

	private final UserService userService;

	private final AuthSystemProperties properties;

	private final InstantSource instantSource;

	public TokenService(UserService userService, AuthSystemProperties properties, InstantSource instantSource) {
		this.userService = userService;
		this.properties = properties;
		this.instantSource = instantSource;
	}

	/**
	 * Generates a new token for the specified username.
	 * @param username the username to generate a token for
	 * @return the generated token string
	 */
	public String generateToken(String username) {
		String token = UUID.randomUUID().toString();
		Instant expiry = this.instantSource.instant().plus(this.properties.tokenExpiry());
		TokenInfo tokenInfo = new TokenInfo(token, username, expiry, false);
		this.tokens.put(token, tokenInfo);
		return token;
	}

	/**
	 * Validates a token and returns the result.
	 * @param token the token to validate
	 * @return the validation result
	 */
	public ValidateResult validate(String token) {
		TokenInfo tokenInfo = this.tokens.get(token);

		if (tokenInfo == null) {
			return new ValidateResult.Failure("TOKEN_NOT_FOUND");
		}

		if (tokenInfo.isExpired(this.instantSource.instant())) {
			return new ValidateResult.Failure("TOKEN_EXPIRED");
		}

		if (tokenInfo.used()) {
			return new ValidateResult.Failure("TOKEN_ALREADY_USED");
		}

		// Mark token as used
		this.tokens.put(token, tokenInfo.markAsUsed());

		// Get user information
		UserInfo userInfo = this.userService.findByUsername(tokenInfo.username()).orElse(null);

		if (userInfo == null) {
			return new ValidateResult.Failure("USER_NOT_FOUND");
		}

		return new ValidateResult.Success(userInfo);
	}

	/**
	 * Sealed interface representing the result of token validation.
	 */
	public sealed interface ValidateResult permits ValidateResult.Success, ValidateResult.Failure {

		record Success(UserInfo userInfo) implements ValidateResult {
		}

		record Failure(String reason) implements ValidateResult {
		}

	}

}
