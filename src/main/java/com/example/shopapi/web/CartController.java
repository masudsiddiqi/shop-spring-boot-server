package com.example.shopapi.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopapi.domain.Cart;
import com.example.shopapi.domain.CartStore;

import jakarta.validation.Valid;

/** The Cart operations of the contract. Every account has its own cart: the owner is the logged-in account. */
@RestController
public class CartController {

	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

	private final CartStore carts;

	public CartController(CartStore carts) {
		this.carts = carts;
	}

	/** operationId: getCart */
	@GetMapping(path = "/cart", produces = JSON)
	public Cart get(Authentication authentication) {
		return carts.view(authentication.getName());
	}

	/** operationId: clearCart */
	@DeleteMapping(path = "/cart", produces = JSON)
	public ResponseEntity<Void> clear(Authentication authentication) {
		carts.clear(authentication.getName());
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	/** operationId: setCartItem */
	@PutMapping(path = "/cart/items/{productId}", consumes = JSON, produces = JSON)
	public Cart setItem(@PathVariable("productId") Integer productId, @Valid @RequestBody CartItemRequest request,
			Authentication authentication) {
		return carts.setItem(authentication.getName(), PathIds.require(productId, "productId"), request.quantity());
	}

	/** operationId: removeCartItem */
	@DeleteMapping(path = "/cart/items/{productId}", produces = JSON)
	public Cart removeItem(@PathVariable("productId") Integer productId, Authentication authentication) {
		return carts.removeItem(authentication.getName(), PathIds.require(productId, "productId"));
	}
}
