package com.example.shopapi.domain;

import java.util.Set;

/**
 * The three roles of an account. A permission is named "resource:action" (for example product:create).
 *
 *   admin     all permissions (the "full permission user" of the test framework)
 *   seller    manages products: read, create, update (cannot delete)
 *   customer  shops: reads products, uses the own cart, places and cancels the own orders
 */
public enum Role {

	admin(Set.of("user:read", "product:read", "product:create", "product:update", "product:delete", "cart:read",
			"cart:update", "order:read", "order:create", "order:cancel", "order:ship")),

	seller(Set.of("product:read", "product:create", "product:update")),

	customer(Set.of("product:read", "cart:read", "cart:update", "order:read", "order:create", "order:cancel"));

	private final Set<String> permissions;

	Role(Set<String> permissions) {
		this.permissions = permissions;
	}

	public Set<String> permissions() {
		return permissions;
	}
}
