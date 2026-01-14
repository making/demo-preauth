package com.example.authsystem.auth;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for managing user information in memory.
 */
@Service
public class UserService {

	private final ConcurrentHashMap<String, UserInfo> users = new ConcurrentHashMap<>();

	public UserService(PasswordEncoder passwordEncoder) {
		// Register demo users
		registerUser(new UserInfo("user1", passwordEncoder.encode("password1"), "User One", List.of("USER")));

		registerUser(
				new UserInfo("admin1", passwordEncoder.encode("password1"), "Admin One", List.of("USER", "ADMIN")));

		registerUser(new UserInfo("user2", passwordEncoder.encode("password2"), "User Two", List.of("USER")));
	}

	/**
	 * Registers a user.
	 * @param userInfo the user information to register
	 */
	public void registerUser(UserInfo userInfo) {
		this.users.put(userInfo.username(), userInfo);
	}

	/**
	 * Finds a user by username.
	 * @param username the username to search for
	 * @return an Optional containing the UserInfo if found, empty otherwise
	 */
	public Optional<UserInfo> findByUsername(String username) {
		return Optional.ofNullable(this.users.get(username));
	}

}
