package com.example.shopapi.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.shopapi.error.ApiException;

/**
 * The accounts, kept in memory. Four accounts exist from the start (password for all: password123):
 *   admin (role admin), seller1 (role seller), customer1 and customer2 (role customer).
 * New accounts are always customers.
 */
public class AccountStore {

	private static final String SEED_TIME = "2026-10-01T09:00:00Z";

	private final Map<String, Account> accounts = new LinkedHashMap<>();
	private int nextId = 1;

	public AccountStore() {
		add("admin", "password123", "admin@shop.example", Role.admin, SEED_TIME);
		add("seller1", "password123", "seller1@shop.example", Role.seller, SEED_TIME);
		add("customer1", "password123", "customer1@shop.example", Role.customer, SEED_TIME);
		add("customer2", "password123", "customer2@shop.example", Role.customer, SEED_TIME);
	}

	private void add(String username, String password, String email, Role role, String createdAt) {
		accounts.put(username, new Account(nextId++, username, password, email, role, createdAt));
	}

	public synchronized Optional<Account> find(String username) {
		return Optional.ofNullable(accounts.get(username));
	}

	public synchronized Optional<Account> authenticate(String username, String password) {
		return find(username).filter(account -> account.password().equals(password));
	}

	/** Registers a customer. The username must be new (the case does not matter). */
	public synchronized Account register(String username, String password, String email) {
		boolean taken = accounts.keySet().stream().anyMatch(name -> name.equalsIgnoreCase(username));
		if (taken) {
			throw ApiException.conflict("An account with this username already exists");
		}
		Account account = new Account(nextId++, username, password, email, Role.customer, Times.now());
		accounts.put(username, account);
		return account;
	}

	/** @param role only accounts with this role; null means every role */
	public synchronized Page<AccountView> list(Role role, int page, int limit) {
		Paging.check(page, limit);
		List<AccountView> matching = new ArrayList<>();
		for (Account account : accounts.values()) {
			if (role == null || account.role() == role) {
				matching.add(AccountView.of(account));
			}
		}
		return Paging.slice(matching, page, limit);
	}
}
