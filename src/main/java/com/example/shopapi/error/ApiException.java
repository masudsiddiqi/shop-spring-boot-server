package com.example.shopapi.error;

/**
 * An error that maps 1:1 to one of the contract's Problem Details responses.
 * Plain Java on purpose (no Spring types), so it can be used from any layer.
 */
public class ApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;
	private final String slug;
	private final String title;

	private ApiException(int status, String slug, String title, String detail) {
		super(detail);
		this.status = status;
		this.slug = slug;
		this.title = title;
	}

	public static ApiException badRequest(String detail) {
		return new ApiException(400, "bad-request", "Bad Request", detail);
	}

	public static ApiException unauthorized(String detail) {
		return new ApiException(401, "unauthorized", "Unauthorized", detail);
	}

	public static ApiException forbidden(String detail) {
		return new ApiException(403, "forbidden", "Forbidden", detail);
	}

	public static ApiException notFound(String detail) {
		return new ApiException(404, "not-found", "Not Found", detail);
	}

	public static ApiException conflict(String detail) {
		return new ApiException(409, "conflict", "Conflict", detail);
	}

	public static ApiException unprocessable(String detail) {
		return new ApiException(422, "unprocessable-entity", "Unprocessable Entity", detail);
	}

	public int status() {
		return status;
	}

	public String slug() {
		return slug;
	}

	public String title() {
		return title;
	}
}
