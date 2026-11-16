package com.example.shopapi.web;

import java.math.BigDecimal;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopapi.domain.Category;
import com.example.shopapi.domain.OrderStore;
import com.example.shopapi.domain.Page;
import com.example.shopapi.domain.Product;
import com.example.shopapi.domain.ProductStore;
import com.example.shopapi.error.ApiException;

import jakarta.validation.Valid;

/** The Products operations of the contract. The permission of each operation is checked in SecurityConfig. */
@RestController
public class ProductController {

	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

	private final ProductStore products;
	private final OrderStore orders;

	public ProductController(ProductStore products, OrderStore orders) {
		this.products = products;
		this.orders = orders;
	}

	/** operationId: listProducts */
	@GetMapping(path = "/products", produces = JSON)
	public Page<Product> list(@RequestParam(name = "category", required = false) Category category,
			@RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
			@RequestParam(name = "page", defaultValue = "1") Integer page,
			@RequestParam(name = "limit", defaultValue = "10") Integer limit) {
		return products.list(category, minPrice, page, limit);
	}

	/** The contract answers a successful creation with 201 and a Location header. operationId: createProduct */
	@PostMapping(path = "/products", consumes = JSON, produces = JSON)
	public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
		Product product = products.create(request.toData());
		return ResponseEntity.created(URI.create("/products/" + product.id())).body(product);
	}

	/** operationId: getProductById */
	@GetMapping(path = "/products/{id}", produces = JSON)
	public Product getById(@PathVariable("id") Integer id) {
		int productId = PathIds.require(id, "id");
		return products.find(productId)
				.orElseThrow(() -> ApiException.notFound("Product with the specified ID does not exist"));
	}

	/** operationId: updateProduct */
	@PutMapping(path = "/products/{id}", consumes = JSON, produces = JSON)
	public Product replace(@PathVariable("id") Integer id, @Valid @RequestBody ProductRequest request) {
		return products.replace(PathIds.require(id, "id"), request.toData());
	}

	/** operationId: partialUpdateProduct */
	@PatchMapping(path = "/products/{id}", consumes = JSON, produces = JSON)
	public Product patch(@PathVariable("id") Integer id, @Valid @RequestBody ProductPatchRequest request) {
		return products.patch(PathIds.require(id, "id"), request.toPatch());
	}

	/** The contract answers a successful delete with 204 and no body. operationId: deleteProduct */
	@DeleteMapping(path = "/products/{id}", produces = JSON)
	public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
		products.delete(PathIds.require(id, "id"), orders::hasActiveOrderWithProduct);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
