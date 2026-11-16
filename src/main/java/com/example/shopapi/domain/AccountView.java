package com.example.shopapi.domain;

/** The "User" schema of the contract. It has no password. */
public record AccountView(int id, String username, String email, String role, String createdAt) {

	public static AccountView of(Account account) {
		return new AccountView(account.id(), account.username(), account.email(), account.role().name(),
				account.createdAt());
	}
}
