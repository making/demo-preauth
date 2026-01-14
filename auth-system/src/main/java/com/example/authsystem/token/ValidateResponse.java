package com.example.authsystem.token;

import java.util.List;

import com.example.authsystem.auth.UserInfo;

/**
 * Response record for token validation API.
 *
 * @param valid whether the token is valid
 * @param username the username if valid, null otherwise
 * @param displayName the display name if valid, null otherwise
 * @param roles the list of roles if valid, null otherwise
 * @param reason the failure reason if invalid, null otherwise
 */
public record ValidateResponse(boolean valid, String username, String displayName, List<String> roles, String reason) {

	/**
	 * Creates a successful validation response.
	 * @param userInfo the user information
	 * @return a ValidateResponse indicating success
	 */
	public static ValidateResponse success(UserInfo userInfo) {
		return new ValidateResponse(true, userInfo.username(), userInfo.displayName(), userInfo.roles(), null);
	}

	/**
	 * Creates a failed validation response.
	 * @param reason the failure reason
	 * @return a ValidateResponse indicating failure
	 */
	public static ValidateResponse failure(String reason) {
		return new ValidateResponse(false, null, null, null, reason);
	}

}
