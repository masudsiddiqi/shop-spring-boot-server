package com.example.shopapi.auth;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Bearer tokens issued by POST /auth/login. The token is an opaque random string kept in memory
 * (the spec says "JWT" only as a format hint). A restart invalidates every token.
 */
@Component
public class TokenStore {

	private final Map<String, String> tokens = new ConcurrentHashMap<>();

	public String issue(String username) {
		String token = UUID.randomUUID().toString();
		tokens.put(token, username);
		return token;
	}

	public Optional<String> usernameFor(String token) {
		return Optional.ofNullable(tokens.get(token));
	}
}
