package com.example.shopapi.domain;

import java.math.BigDecimal;
import java.util.List;

/** The "Cart" schema of the contract. */
public record Cart(List<LineItem> items, BigDecimal total) {
}
