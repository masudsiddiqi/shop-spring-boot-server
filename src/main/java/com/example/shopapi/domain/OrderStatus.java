package com.example.shopapi.domain;

/** placed -> shipped (admin), placed -> cancelled (owner or admin). Shipped and cancelled are final. */
public enum OrderStatus {
	placed, shipped, cancelled
}
