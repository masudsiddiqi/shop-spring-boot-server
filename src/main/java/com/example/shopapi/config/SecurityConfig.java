package com.example.shopapi.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.shopapi.auth.ApiAuthFilter;
import com.example.shopapi.auth.TokenStore;
import com.example.shopapi.domain.AccountStore;
import com.example.shopapi.error.JsonErrors;

/**
 * Security layer: who is the caller (401) and may the caller do this (403).
 * It runs before routing, so an unauthenticated request gets 401 even for a path that does not exist.
 *
 * The permission is named "resource:action" (see the Role enum). The role of the account decides
 * which permissions it has. The ownership rules ("a customer sees only the own orders") are not here:
 * they need the data, so the OrderStore applies them.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AccountStore accounts, TokenStore tokens)
			throws Exception {
		http
				// CORS first: the browser sends a preflight OPTIONS request before the real request,
				// and it carries no token. CORS must answer it before the authentication runs.
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.logout(logout -> logout.disable())
				.authorizeHttpRequests(rules -> rules
						// public endpoints (the spec sets "security: []" on them)
						.requestMatchers("/ping", "/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/users").permitAll()
						// any logged-in account can read its own data
						.requestMatchers(HttpMethod.GET, "/users/me").authenticated()
						// users
						.requestMatchers(HttpMethod.GET, "/users").hasAuthority("user:read")
						// products
						.requestMatchers(HttpMethod.GET, "/products", "/products/**").hasAuthority("product:read")
						.requestMatchers(HttpMethod.POST, "/products").hasAuthority("product:create")
						.requestMatchers(HttpMethod.PUT, "/products/**").hasAuthority("product:update")
						.requestMatchers(HttpMethod.PATCH, "/products/**").hasAuthority("product:update")
						.requestMatchers(HttpMethod.DELETE, "/products/**").hasAuthority("product:delete")
						// cart
						.requestMatchers(HttpMethod.GET, "/cart").hasAuthority("cart:read")
						.requestMatchers(HttpMethod.PUT, "/cart/items/**").hasAuthority("cart:update")
						.requestMatchers(HttpMethod.DELETE, "/cart", "/cart/items/**").hasAuthority("cart:update")
						// orders
						.requestMatchers(HttpMethod.POST, "/orders").hasAuthority("order:create")
						.requestMatchers(HttpMethod.GET, "/orders", "/orders/**").hasAuthority("order:read")
						.requestMatchers(HttpMethod.POST, "/orders/*/cancel").hasAuthority("order:cancel")
						.requestMatchers(HttpMethod.POST, "/orders/*/ship").hasAuthority("order:ship")
						// everything else only needs a valid login (so an unknown path is 404, not 401, for a caller who is logged in)
						.anyRequest().authenticated())
				.addFilterBefore(new ApiAuthFilter(accounts, tokens), AuthorizationFilter.class)
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, exception) -> {
							response.setHeader("WWW-Authenticate", "Bearer");
							JsonErrors.write(request, response, 401, "unauthorized", "Unauthorized",
									"Authentication token is missing or expired");
						})
						.accessDeniedHandler((request, response, exception) -> JsonErrors.write(request, response, 403,
								"forbidden", "Forbidden", "You do not have required permission/role for this operation")));
		return http.build();
	}

	// Which web pages may call this API from a browser (for example Swagger UI "Try it out").
	// A tool such as curl or RestAssured is not a browser and does not need this.
	// The list is for local testing. Add another origin here if you use another Swagger page.
	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOriginPatterns(List.of("https://editor.swagger.io", "http://localhost:*", "http://127.0.0.1:*"));
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("*"));
		cors.setExposedHeaders(List.of("Location", "Allow", "WWW-Authenticate"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cors);
		return source;
	}
}
