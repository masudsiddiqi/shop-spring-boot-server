package com.example.shopapi.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.shopapi.error.JsonErrors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rejects a request whose body is bigger than the limit with 413, BEFORE security, routing or any
 * controller code runs. It plays the role of the gateway / reverse proxy in front of the real system:
 * Spring Boot has no built-in size limit for JSON bodies (the Tomcat form-post limit applies to
 * form data only).
 *
 * It checks the Content-Length header, so it does not cover chunked requests without a length.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PayloadSizeFilter extends OncePerRequestFilter {

	private final long maxBytes;

	public PayloadSizeFilter(@Value("${app.max-payload-bytes:10485760}") long maxBytes) {
		this.maxBytes = maxBytes;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (request.getContentLengthLong() > maxBytes) {
			JsonErrors.write(request, response, 413, "payload-too-large", "Payload Too Large",
					"Request payload exceeds maximum allowed size");
			return;
		}
		chain.doFilter(request, response);
	}
}
