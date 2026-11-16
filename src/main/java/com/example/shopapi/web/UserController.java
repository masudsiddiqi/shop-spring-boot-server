package com.example.shopapi.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopapi.domain.Account;
import com.example.shopapi.domain.AccountStore;
import com.example.shopapi.domain.AccountView;
import com.example.shopapi.domain.Page;
import com.example.shopapi.domain.Role;
import com.example.shopapi.error.ApiException;

import jakarta.validation.Valid;

/**
 * The Users operations of the contract.
 *
 * "consumes" and "produces" are declared on purpose. Because of them, Spring itself rejects a wrong
 * Content-Type with 415 and a wrong Accept header with 406 while it picks the controller method.
 */
@RestController
public class UserController {

	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

	private final AccountStore accounts;

	public UserController(AccountStore accounts) {
		this.accounts = accounts;
	}

	/** Public registration: always a customer. operationId: registerUser */
	@PostMapping(path = "/users", consumes = JSON, produces = JSON)
	public ResponseEntity<AccountView> register(@Valid @RequestBody UserRegistrationRequest request) {
		Account account = accounts.register(request.username(), request.password(), request.email());
		return ResponseEntity.status(HttpStatus.CREATED).body(AccountView.of(account));
	}

	/** Admin only (permission user:read, checked in SecurityConfig). operationId: listUsers */
	@GetMapping(path = "/users", produces = JSON)
	public Page<AccountView> list(@RequestParam(name = "role", required = false) Role role,
			@RequestParam(name = "page", defaultValue = "1") Integer page,
			@RequestParam(name = "limit", defaultValue = "10") Integer limit) {
		return accounts.list(role, page, limit);
	}

	/** The logged-in account. operationId: getCurrentUser */
	@GetMapping(path = "/users/me", produces = JSON)
	public AccountView me(Authentication authentication) {
		return accounts.find(authentication.getName()).map(AccountView::of)
				.orElseThrow(() -> ApiException.unauthorized("Authentication token is missing or expired"));
	}
}
