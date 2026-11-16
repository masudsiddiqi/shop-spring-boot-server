package com.example.shopapi.error;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Turns every failure that happens inside Spring MVC (routing, media-type matching, argument binding,
 * validation, the controllers) into the contract's Problem Details body.
 *
 * Every body is written with an explicit "application/json" content type. Without it, a request with
 * "Accept: application/xml" would make Spring try to write the error body as XML and fail.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	private static ResponseEntity<ProblemDetails> problem(HttpServletRequest request, int status, String slug,
			String title, String detail) {
		return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
				.body(ProblemDetails.of(status, slug, title, detail, request.getRequestURI()));
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ProblemDetails> handleApi(ApiException ex, HttpServletRequest request) {
		return problem(request, ex.status(), ex.slug(), ex.title(), ex.getMessage());
	}

	// 422: the JSON is readable, but a field breaks a constraint (required, length, range, format)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetails> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String problems = Stream
				.concat(ex.getBindingResult().getFieldErrors().stream()
						.map(error -> error.getField() + " " + error.getDefaultMessage()),
						ex.getBindingResult().getGlobalErrors().stream().map(error -> error.getDefaultMessage()))
				.collect(Collectors.joining("; "));
		return problem(request, 422, "unprocessable-entity", "Unprocessable Entity", "Validation failed: " + problems);
	}

	// 400: the body cannot be read at all (broken JSON, no body, wrong field type, unknown field, unknown enum value)
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetails> handleUnreadable(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		return problem(request, 400, "bad-request", "Bad Request",
				"Invalid JSON syntax, missing body, or field type mismatch");
	}

	// 400: a path or query parameter has the wrong type (for example /products/abc, or ?category=banana)
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ProblemDetails> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		String kind = ex.getParameter().hasParameterAnnotation(PathVariable.class) ? "path parameter"
				: "query parameter";
		String type = ex.getRequiredType() == null ? "the expected type" : ex.getRequiredType().getSimpleName();
		return problem(request, 400, "bad-request", "Bad Request",
				"Failed to convert " + kind + " '" + ex.getName() + "' to required type '" + type + "'");
	}

	// 400: a required query parameter is missing
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ProblemDetails> handleMissingParameter(MissingServletRequestParameterException ex,
			HttpServletRequest request) {
		return problem(request, 400, "bad-request", "Bad Request",
				"Required query parameter '" + ex.getParameterName() + "' is missing");
	}

	// 400: a required header is missing (for example Idempotency-Key)
	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ProblemDetails> handleMissingHeader(MissingRequestHeaderException ex,
			HttpServletRequest request) {
		return problem(request, 400, "bad-request", "Bad Request",
				"Required header '" + ex.getHeaderName() + "' is missing");
	}

	// 415: the Content-Type is not one the endpoint consumes (or is missing)
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ProblemDetails> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
			HttpServletRequest request) {
		return problem(request, 415, "unsupported-media-type", "Unsupported Media Type",
				"Unsupported or missing Content-Type header. Expected 'application/json'");
	}

	// 406: the Accept header asks for a representation the endpoint does not produce
	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	public ResponseEntity<ProblemDetails> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex,
			HttpServletRequest request) {
		return problem(request, 406, "not-acceptable", "Not Acceptable",
				"Could not find acceptable representation matching requested Accept header");
	}

	// 405: the path exists but not for this HTTP method. RFC 9110 requires the Allow header.
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ProblemDetails> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		Set<HttpMethod> supported = ex.getSupportedHttpMethods();
		ResponseEntity.BodyBuilder builder = ResponseEntity.status(405).contentType(MediaType.APPLICATION_JSON);
		String detail = "HTTP method " + ex.getMethod() + " is not supported for this endpoint";
		if (supported != null && !supported.isEmpty()) {
			builder.allow(supported.toArray(new HttpMethod[0]));
			detail = detail + ". Supported: "
					+ supported.stream().map(HttpMethod::name).sorted().collect(Collectors.joining(", "));
		}
		return builder.body(ProblemDetails.of(405, "method-not-allowed", "Method Not Allowed", detail,
				request.getRequestURI()));
	}

	// 404: no endpoint matches the path (which of the two exceptions is thrown depends on the Spring version)
	@ExceptionHandler({ NoHandlerFoundException.class, NoResourceFoundException.class })
	public ResponseEntity<ProblemDetails> handleNoEndpoint(Exception ex, HttpServletRequest request) {
		return problem(request, 404, "not-found", "Not Found",
				"No endpoint matches " + request.getMethod() + " " + request.getRequestURI());
	}

	// 500: anything unexpected. Not part of the contract, but the server must still answer with JSON.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetails> handleUnexpected(Exception ex, HttpServletRequest request) {
		return problem(request, 500, "internal-server-error", "Internal Server Error",
				"An unexpected error occurred");
	}
}
