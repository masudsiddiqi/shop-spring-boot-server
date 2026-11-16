package com.example.shopapi.domain;

import java.util.List;

/** One page of a list. The fields are the ones of the contract: items, page, limit, total. */
public record Page<T>(List<T> items, int page, int limit, long total) {
}
