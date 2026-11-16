package com.example.shopapi.domain;

import java.math.BigDecimal;
import java.util.List;

/** The "Order" schema of the contract. A null note is not written. */
public record Order(int id, OrderStatus status, String customer, List<LineItem> items, BigDecimal total,
		Address shippingAddress, String note, String createdAt) {

	public Order withStatus(OrderStatus newStatus) {
		return new Order(id, newStatus, customer, items, total, shippingAddress, note, createdAt);
	}
}
