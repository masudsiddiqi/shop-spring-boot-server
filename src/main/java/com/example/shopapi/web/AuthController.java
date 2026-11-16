package com.example.shopapi.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopapi.auth.TokenStore;
import com.example.shopapi.domain.Account;
import com.example.shopapi.domain.AccountStore;
import com.example.shopapi.error.ApiException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
public class AuthController {

	public record LoginRequest(@NotBlank String username, @NotBlank String password) {
	}

	public record AuthTokenResponse(String token) {
	}

	private final AccountStore accounts;
	private final TokenStore tokens;

	public AuthController(AccountStore accounts, TokenStore tokens) {
		this.accounts = accounts;
		this.tokens = tokens;
	}

	/** Exchanges a username/password for a bearer token (public). operationId: loginUser */
	@PostMapping(path = "/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
		Account account = accounts.authenticate(request.username(), request.password())
				.orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));
		return new AuthTokenResponse(tokens.issue(account.username()));
	}
}
