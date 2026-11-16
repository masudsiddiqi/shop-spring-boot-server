package com.example.shopapi.domain;

import java.util.List;

import com.example.shopapi.error.ApiException;

/** The paging rules of the contract: page is 1 or more, limit is 1 to 50. A breach is a 422. */
public final class Paging {

	public static final int MAX_LIMIT = 50;

	private Paging() {
	}

	public static void check(int page, int limit) {
		if (page < 1) {
			throw ApiException.unprocessable("Validation failed: page must be 1 or more");
		}
		if (limit < 1 || limit > MAX_LIMIT) {
			throw ApiException.unprocessable("Validation failed: limit must be between 1 and " + MAX_LIMIT);
		}
	}

	/** A page beyond the last one is not an error: it is an empty page. */
	public static <T> Page<T> slice(List<T> all, int page, int limit) {
		long from = (long) (page - 1) * limit;
		List<T> items = from >= all.size() ? List.of()
				: all.subList((int) from, (int) Math.min(all.size(), from + limit));
		return new Page<>(List.copyOf(items), page, limit, all.size());
	}
}
