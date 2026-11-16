package com.example.shopapi.web;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

	/** Health check (public). BaseTest calls it before every test class. operationId: healthCheck */
	@GetMapping(path = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> ping() {
		return Map.of("status", "UP");
	}
}
