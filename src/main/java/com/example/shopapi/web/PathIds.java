package com.example.shopapi.web;

import com.example.shopapi.error.ApiException;

/** The contract says a path id is an integer of 1 or more. A smaller number is a 400, like a wrong type. */
final class PathIds {

	private PathIds() {
	}

	static int require(Integer value, String name) {
		if (value == null || value < 1) {
			throw ApiException.badRequest("Path parameter '" + name + "' must be 1 or more");
		}
		return value;
	}
}
