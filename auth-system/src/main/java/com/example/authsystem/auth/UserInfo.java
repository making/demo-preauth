package com.example.authsystem.auth;

import java.util.List;

/**
 * Immutable record representing user information.
 *
 * @param username the unique username
 * @param password the BCrypt hashed password
 * @param displayName the display name shown to users
 * @param roles the list of roles assigned to the user (e.g., "USER", "ADMIN")
 */
public record UserInfo(String username, String password, String displayName, List<String> roles) {
}
