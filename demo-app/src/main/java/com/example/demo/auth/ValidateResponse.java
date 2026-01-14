package com.example.demo.auth;

import java.util.List;

/**
 * Response from auth-system's token validation API.
 */
public record ValidateResponse(boolean valid, String username, String displayName, List<String> roles, String reason) {
}
