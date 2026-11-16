package com.example.shopapi.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.shopapi.error.ApiException;

/** The carts, kept in memory. Every account has its own cart. The prices are the current prices of the products. */
public class CartStore {

	private final ProductStore products;
	private final Map<String, Map<Integer, Integer>> carts = new HashMap<>();

	public CartStore(ProductStore products) {
		this.products = products;
	}

	public synchronized Cart view(String username) {
		Map<Integer, Integer> quantities = carts.getOrDefault(username, Map.of());
		List<LineItem> lines = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO.setScale(2);
		for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
			Optional<Product> product = products.find(entry.getKey());
			if (product.isEmpty()) {
				continue;
			}
			BigDecimal lineTotal = product.get().price().multiply(BigDecimal.valueOf(entry.getValue())).setScale(2,
					RoundingMode.HALF_UP);
			lines.add(new LineItem(product.get().id(), product.get().name(), product.get().price(), entry.getValue(),
					lineTotal));
			total = total.add(lineTotal);
		}
		return new Cart(List.copyOf(lines), total);
	}

	/** Adds the product, or replaces its quantity. The quantity cannot be more than the stock (409). */
	public synchronized Cart setItem(String username, int productId, int quantity) {
		if (quantity < 1 || quantity > 99) {
			throw ApiException.unprocessable("Validation failed: quantity must be between 1 and 99");
		}
		Product product = products.find(productId)
				.orElseThrow(() -> ApiException.notFound("Product with the specified ID does not exist"));
		if (quantity > product.stock()) {
			throw ApiException.conflict("Not enough stock: only " + product.stock() + " available");
		}
		carts.computeIfAbsent(username, name -> new LinkedHashMap<>()).put(productId, quantity);
		return view(username);
	}

	public synchronized Cart removeItem(String username, int productId) {
		Map<Integer, Integer> quantities = carts.get(username);
		if (quantities == null || quantities.remove(productId) == null) {
			throw ApiException.notFound("The product is not in the cart");
		}
		return view(username);
	}

	public synchronized void clear(String username) {
		carts.remove(username);
	}
}
