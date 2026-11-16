package com.example.shopapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.shopapi.domain.AccountStore;
import com.example.shopapi.domain.CartStore;
import com.example.shopapi.domain.OrderStore;
import com.example.shopapi.domain.ProductStore;

/**
 * Creates the stores. The stores are plain Java classes (no Spring annotation), so their rules
 * (stock, ownership, idempotency, state changes) can be read and tested without Spring.
 */
@Configuration
public class ShopConfig {

	@Bean
	AccountStore accountStore() {
		return new AccountStore();
	}

	@Bean
	ProductStore productStore() {
		return new ProductStore();
	}

	@Bean
	CartStore cartStore(ProductStore products) {
		return new CartStore(products);
	}

	@Bean
	OrderStore orderStore(ProductStore products, CartStore carts) {
		return new OrderStore(products, carts);
	}
}
