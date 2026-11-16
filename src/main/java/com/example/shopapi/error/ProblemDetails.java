package com.example.shopapi.error;

/**
 * The error body of the contract (RFC 9457 Problem Details), exactly the fields of the
 * "ProblemDetails" schema: type, title, status, detail, instance.
 */
public record ProblemDetails(String type, String title, int status, String detail, String instance) {

	private static final String TYPE_BASE = "https://example.com/probs/";

	public static ProblemDetails of(int status, String slug, String title, String detail, String instance) {
		return new ProblemDetails(TYPE_BASE + slug, title, status, detail, instance);
	}
}
