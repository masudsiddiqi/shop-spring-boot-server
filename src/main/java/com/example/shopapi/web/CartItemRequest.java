package com.example.shopapi.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The "CartItemRequest" schema (PUT /cart/items/{productId}). */
public record CartItemRequest(@NotNull @Min(1) @Max(99) Integer quantity) {
}
