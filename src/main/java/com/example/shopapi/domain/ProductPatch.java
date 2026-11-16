package com.example.shopapi.domain;

import java.math.BigDecimal;

/** The fields of a PATCH. A null field means "not sent": it is left as it is. */
public record ProductPatch(String sku, String name, String description, Category category, BigDecimal price,
		Integer stock) {
}
