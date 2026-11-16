package com.example.shopapi.web;

import java.math.BigDecimal;

import com.example.shopapi.domain.Category;
import com.example.shopapi.domain.ProductPatch;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The "ProductPatchRequest" schema (PATCH /products/{id}): every field is optional.
 * A field that is sent follows the same rules as in ProductRequest.
 */
public record ProductPatchRequest(
		@Pattern(regexp = "[A-Z0-9-]{3,20}") String sku,
		@Size(min = 1, max = 100) String name,
		@Size(max = 500) String description,
		Category category,
		@DecimalMin("0.01") @DecimalMax("9999999.99") @Digits(integer = 7, fraction = 2) BigDecimal price,
		@Min(0) Integer stock) {

	public ProductPatch toPatch() {
		return new ProductPatch(sku, name, description, category, price, stock);
	}
}
