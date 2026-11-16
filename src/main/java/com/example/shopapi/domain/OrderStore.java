package com.example.shopapi.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.example.shopapi.error.ApiException;

/**
 * The orders, kept in memory.
 *
 * Placing an order takes the items of the cart, lowers the stock of the products and empties the cart.
 * The Idempotency-Key makes a repeated request safe: the same key with the same body returns the first order.
 */
public class OrderStore {

	/** @param replayed true when the key was used before and the first order is returned again */
	public record CreateResult(Order order, boolean replayed) {
	}

	private record Used(String fingerprint, int orderId) {
	}

	private final ProductStore products;
	private final CartStore carts;
	private final Map<Integer, Order> orders = new TreeMap<>();
	private final Map<String, Used> usedKeys = new HashMap<>();
	private int nextId = 1;

	public OrderStore(ProductStore products, CartStore carts) {
		this.products = products;
		this.carts = carts;
	}

	public synchronized CreateResult create(String username, String idempotencyKey, Address address, String note) {
		if (idempotencyKey == null || idempotencyKey.length() < 8 || idempotencyKey.length() > 64) {
			throw ApiException.unprocessable("Validation failed: Idempotency-Key must have 8 to 64 characters");
		}
		String fingerprint = address.street() + "|" + address.city() + "|" + address.postalCode() + "|"
				+ address.country() + "|" + (note == null ? "" : note);
		String scopedKey = username + "\n" + idempotencyKey;

		Used used = usedKeys.get(scopedKey);
		if (used != null) {
			if (used.fingerprint().equals(fingerprint)) {
				return new CreateResult(orders.get(used.orderId()), true);
			}
			throw ApiException.conflict("The Idempotency-Key was already used with a different request");
		}

		Cart cart = carts.view(username);
		if (cart.items().isEmpty()) {
			throw ApiException.conflict("The cart is empty");
		}
		for (LineItem line : cart.items()) {
			int stock = products.find(line.productId()).map(Product::stock).orElse(0);
			if (stock < line.quantity()) {
				throw ApiException.conflict(
						"Not enough stock for product " + line.productId() + ": only " + stock + " available");
			}
		}
		for (LineItem line : cart.items()) {
			products.adjustStock(line.productId(), -line.quantity());
		}

		Order order = new Order(nextId, OrderStatus.placed, username, cart.items(), cart.total(), address, note,
				Times.now());
		orders.put(nextId, order);
		nextId++;
		usedKeys.put(scopedKey, new Used(fingerprint, order.id()));
		carts.clear(username);
		return new CreateResult(order, false);
	}

	/**
	 * @param admin an admin can see every order; any other account only its own orders (else 403)
	 */
	public synchronized Order requireVisible(int id, String username, boolean admin) {
		Order order = orders.get(id);
		if (order == null) {
			throw ApiException.notFound("Order with the specified ID does not exist");
		}
		if (!admin && !order.customer().equals(username)) {
			throw ApiException.forbidden("You are not allowed to access this order");
		}
		return order;
	}

	/**
	 * @param owner only the orders of this username; null means every order (an admin)
	 * @param status only this status; null means every status
	 */
	public synchronized Page<Order> list(String owner, OrderStatus status, int page, int limit) {
		Paging.check(page, limit);
		List<Order> matching = new ArrayList<>();
		for (Order order : orders.values()) {
			boolean ownerOk = owner == null || order.customer().equals(owner);
			boolean statusOk = status == null || order.status() == status;
			if (ownerOk && statusOk) {
				matching.add(order);
			}
		}
		return Paging.slice(matching, page, limit);
	}

	/** Only a placed order can be cancelled (else 409). The stock of its products goes up again. */
	public synchronized Order cancel(int id, String username, boolean admin) {
		Order order = requireVisible(id, username, admin);
		if (order.status() != OrderStatus.placed) {
			throw ApiException.conflict("Only a placed order can be cancelled (current status: " + order.status() + ")");
		}
		for (LineItem line : order.items()) {
			products.adjustStock(line.productId(), line.quantity());
		}
		Order cancelled = order.withStatus(OrderStatus.cancelled);
		orders.put(id, cancelled);
		return cancelled;
	}

	/** Only a placed order can be shipped (else 409). Only an admin calls this. */
	public synchronized Order ship(int id) {
		Order order = Optional.ofNullable(orders.get(id))
				.orElseThrow(() -> ApiException.notFound("Order with the specified ID does not exist"));
		if (order.status() != OrderStatus.placed) {
			throw ApiException.conflict("Only a placed order can be shipped (current status: " + order.status() + ")");
		}
		Order shipped = order.withStatus(OrderStatus.shipped);
		orders.put(id, shipped);
		return shipped;
	}

	/** True when the product is in an order that is placed or shipped. Such a product cannot be deleted. */
	public synchronized boolean hasActiveOrderWithProduct(int productId) {
		for (Order order : orders.values()) {
			if (order.status() != OrderStatus.cancelled
					&& order.items().stream().anyMatch(line -> line.productId() == productId)) {
				return true;
			}
		}
		return false;
	}
}
