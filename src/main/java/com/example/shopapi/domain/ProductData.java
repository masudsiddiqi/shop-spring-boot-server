package com.example.shopapi.domain;

import java.math.BigDecimal;

/** The fields of a new or replaced product (POST and PUT). */
public record ProductData(String sku, String name, String description, Category category, BigDecimal price,
		int stock) {
}
