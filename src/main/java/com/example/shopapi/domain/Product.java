package com.example.shopapi.domain;

import java.math.BigDecimal;

/** The "Product" schema of the contract. A null description is not written. */
public record Product(int id, String sku, String name, String description, Category category, BigDecimal price,
		int stock, String createdAt) {
}
