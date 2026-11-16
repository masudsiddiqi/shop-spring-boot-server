package com.example.shopapi.error;

import java.io.IOException;

import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes a Problem Details body straight to the servlet response. It is used where no controller
 * (and so no @RestControllerAdvice) is involved: the servlet filters and the security handlers.
 */
public final class JsonErrors {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private JsonErrors() {
	}

	public static void write(HttpServletRequest request, HttpServletResponse response, int status, String slug,
			String title, String detail) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		MAPPER.writeValue(response.getOutputStream(),
				ProblemDetails.of(status, slug, title, detail, request.getRequestURI()));
	}
}
