package com.example.shopapi.web;

import java.math.BigDecimal;

import com.example.shopapi.domain.Category;
import com.example.shopapi.domain.ProductData;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The "ProductRequest" schema (POST /products and PUT /products/{id}).
 * A missing required field or a constraint breach is a 422; a wrong JSON type or an unknown category is a 400.
 */
public record ProductRequest(
		@NotNull @Pattern(regexp = "[A-Z0-9-]{3,20}") String sku,
		@NotNull @Size(min = 1, max = 100) String name,
		@Size(max = 500) String description,
		@NotNull Category category,
		@NotNull @DecimalMin("0.01") @DecimalMax("9999999.99") @Digits(integer = 7, fraction = 2) BigDecimal price,
		@NotNull @Min(0) Integer stock) {

	public ProductData toData() {
		return new ProductData(sku, name, description, category, price, stock);
	}
}
