package com.example.shopapi.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.IntPredicate;

import com.example.shopapi.error.ApiException;

/**
 * The catalogue, kept in memory. Five products exist from the start; the last one has no stock.
 * The sku is unique (a duplicate is a 409).
 */
public class ProductStore {

	private static final String SEED_TIME = "2026-10-01T09:00:00Z";

	private final Map<Integer, Product> products = new TreeMap<>();
	private int nextId = 1;

	public ProductStore() {
		seed("BOOK-001", "Clean Code", "A handbook of agile software craftsmanship", Category.books, "29.99", 50);
		seed("BOOK-002", "The Pragmatic Programmer", "Your journey to mastery", Category.books, "34.50", 30);
		seed("ELEC-001", "USB-C Cable", "1 m braided cable", Category.electronics, "9.99", 200);
		seed("ELEC-002", "Wireless Mouse", "2.4 GHz, silent click", Category.electronics, "19.99", 75);
		seed("HOME-001", "Desk Lamp", "LED lamp with an adjustable arm", Category.home, "24.00", 0);
	}

	private void seed(String sku, String name, String description, Category category, String price, int stock) {
		products.put(nextId, new Product(nextId, sku, name, description, category, money(new BigDecimal(price)),
				stock, SEED_TIME));
		nextId++;
	}

	private static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	public synchronized Optional<Product> find(int id) {
		return Optional.ofNullable(products.get(id));
	}

	private Product require(int id) {
		return find(id).orElseThrow(() -> ApiException.notFound("Product with the specified ID does not exist"));
	}

	/** @param category only this category (null = all); @param minPrice only a price of at least this (null = any) */
	public synchronized Page<Product> list(Category category, BigDecimal minPrice, int page, int limit) {
		Paging.check(page, limit);
		if (minPrice != null && minPrice.signum() < 0) {
			throw ApiException.unprocessable("Validation failed: minPrice must be 0 or more");
		}
		List<Product> matching = new ArrayList<>();
		for (Product product : products.values()) {
			boolean categoryOk = category == null || product.category() == category;
			boolean priceOk = minPrice == null || product.price().compareTo(minPrice) >= 0;
			if (categoryOk && priceOk) {
				matching.add(product);
			}
		}
		return Paging.slice(matching, page, limit);
	}

	public synchronized Product create(ProductData data) {
		requireFreeSku(data.sku(), -1);
		Product product = new Product(nextId, data.sku(), data.name(), data.description(), data.category(),
				money(data.price()), data.stock(), Times.now());
		products.put(nextId, product);
		nextId++;
		return product;
	}

	public synchronized Product replace(int id, ProductData data) {
		Product old = require(id);
		requireFreeSku(data.sku(), id);
		Product product = new Product(id, data.sku(), data.name(), data.description(), data.category(),
				money(data.price()), data.stock(), old.createdAt());
		products.put(id, product);
		return product;
	}

	public synchronized Product patch(int id, ProductPatch patch) {
		Product old = require(id);
		String sku = patch.sku() != null ? patch.sku() : old.sku();
		requireFreeSku(sku, id);
		Product product = new Product(id, sku, patch.name() != null ? patch.name() : old.name(),
				patch.description() != null ? patch.description() : old.description(),
				patch.category() != null ? patch.category() : old.category(),
				patch.price() != null ? money(patch.price()) : old.price(),
				patch.stock() != null ? patch.stock() : old.stock(), old.createdAt());
		products.put(id, product);
		return product;
	}

	/** @param inActiveOrder tells whether a product is in an order that is placed or shipped (then: 409) */
	public synchronized void delete(int id, IntPredicate inActiveOrder) {
		require(id);
		if (inActiveOrder.test(id)) {
			throw ApiException.conflict("The product is in an order that is placed or shipped");
		}
		products.remove(id);
	}

	/** Changes the stock of a product (used by the orders). A product that does not exist any more is ignored. */
	synchronized void adjustStock(int id, int delta) {
		Product old = products.get(id);
		if (old != null) {
			products.put(id, new Product(id, old.sku(), old.name(), old.description(), old.category(), old.price(),
					old.stock() + delta, old.createdAt()));
		}
	}

	private void requireFreeSku(String sku, int ownId) {
		for (Product product : products.values()) {
			if (product.sku().equals(sku) && product.id() != ownId) {
				throw ApiException.conflict("A product with the sku '" + sku + "' already exists");
			}
		}
	}
}
