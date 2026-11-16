package com.example.shopapi.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** The time stamps of the contract ("format: date-time"), for example 2026-10-24T10:15:00Z. */
final class Times {

	private Times() {
	}

	static String now() {
		return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
	}
}
