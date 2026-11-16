package com.example.shopapi.web;

import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopapi.domain.AccountStore;
import com.example.shopapi.domain.Order;
import com.example.shopapi.domain.OrderStatus;
import com.example.shopapi.domain.OrderStore;
import com.example.shopapi.domain.Page;
import com.example.shopapi.domain.Role;

import jakarta.validation.Valid;

/**
 * The Orders operations of the contract. The permission of each operation is checked in SecurityConfig.
 * The ownership rule is applied here and in the OrderStore: a customer works only with the own orders,
 * an admin works with every order.
 */
@RestController
public class OrderController {

	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

	private final OrderStore orders;
	private final AccountStore accounts;

	public OrderController(OrderStore orders, AccountStore accounts) {
		this.orders = orders;
		this.accounts = accounts;
	}

	private boolean isAdmin(Authentication authentication) {
		return accounts.find(authentication.getName()).map(account -> account.role() == Role.admin).orElse(false);
	}

	/**
	 * Places an order from the cart. 201 for a new order, 200 when the same Idempotency-Key and the same body
	 * were sent before (the first order is returned). operationId: createOrder
	 */
	@PostMapping(path = "/orders", consumes = JSON, produces = JSON)
	public ResponseEntity<Order> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody OrderRequest request, Authentication authentication) {
		OrderStore.CreateResult result = orders.create(authentication.getName(), idempotencyKey,
				request.shippingAddress().toAddress(), request.note());
		if (result.replayed()) {
			return ResponseEntity.ok(result.order());
		}
		return ResponseEntity.created(URI.create("/orders/" + result.order().id())).body(result.order());
	}

	/** operationId: listOrders */
	@GetMapping(path = "/orders", produces = JSON)
	public Page<Order> list(@RequestParam(name = "status", required = false) OrderStatus status,
			@RequestParam(name = "page", defaultValue = "1") Integer page,
			@RequestParam(name = "limit", defaultValue = "10") Integer limit, Authentication authentication) {
		String owner = isAdmin(authentication) ? null : authentication.getName();
		return orders.list(owner, status, page, limit);
	}

	/** operationId: getOrderById */
	@GetMapping(path = "/orders/{id}", produces = JSON)
	public Order getById(@PathVariable("id") Integer id, Authentication authentication) {
		return orders.requireVisible(PathIds.require(id, "id"), authentication.getName(), isAdmin(authentication));
	}

	/** operationId: cancelOrder */
	@PostMapping(path = "/orders/{id}/cancel", produces = JSON)
	public Order cancel(@PathVariable("id") Integer id, Authentication authentication) {
		return orders.cancel(PathIds.require(id, "id"), authentication.getName(), isAdmin(authentication));
	}

	/** Admin only (permission order:ship, checked in SecurityConfig). operationId: shipOrder */
	@PostMapping(path = "/orders/{id}/ship", produces = JSON)
	public Order ship(@PathVariable("id") Integer id) {
		return orders.ship(PathIds.require(id, "id"));
	}
}
