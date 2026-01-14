package com.example.authsystem.token;

import java.time.Instant;

/**
 * Immutable record representing token information.
 *
 * @param token the token string (UUID format)
 * @param username the username associated with this token
 * @param expiry the expiration time of this token
 * @param used whether this token has been used
 */
public record TokenInfo(String token, String username, Instant expiry, boolean used) {

	/**
	 * Creates a new TokenInfo instance marked as used.
	 * @return a new TokenInfo with used=true
	 */
	public TokenInfo markAsUsed() {
		return new TokenInfo(this.token, this.username, this.expiry, true);
	}

	/**
	 * Checks if this token has expired.
	 * @param now the current time to check against
	 * @return true if the given time is after the expiry time
	 */
	public boolean isExpired(Instant now) {
		return now.isAfter(this.expiry);
	}

}
