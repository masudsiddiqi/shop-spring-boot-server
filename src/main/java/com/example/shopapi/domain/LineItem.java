package com.example.shopapi.domain;

import java.math.BigDecimal;

/** One line of a cart or an order: the "LineItem" schema of the contract. */
public record LineItem(int productId, String name, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
}
