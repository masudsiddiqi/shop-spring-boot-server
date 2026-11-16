package com.example.shopapi.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The "OrderRequest" schema (POST /orders). The items come from the cart, not from the body. */
public record OrderRequest(@NotNull @Valid AddressRequest shippingAddress, @Size(max = 200) String note) {
}
