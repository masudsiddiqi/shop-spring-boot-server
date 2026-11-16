package com.example.shopapi.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.shopapi.domain.Account;
import com.example.shopapi.domain.AccountStore;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads the Authorization header of every request and, when it holds valid credentials,
 * puts the account into the security context. The permissions come from the role of the account.
 *
 *   Bearer <token>              the token must have been issued by POST /auth/login
 *   Basic <base64(user:pass)>   a non-base64 value, or a decoded value without a ":", is invalid
 *
 * An absent or invalid header does NOT fail here: the request simply stays anonymous, and the
 * authorization rules in SecurityConfig answer it with 401.
 */
public class ApiAuthFilter extends OncePerRequestFilter {

	private final AccountStore accounts;
	private final TokenStore tokens;

	public ApiAuthFilter(AccountStore accounts, TokenStore tokens) {
		this.accounts = accounts;
		this.tokens = tokens;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null) {
			Account account = resolve(header);
			if (account != null) {
				var authentication = new UsernamePasswordAuthenticationToken(account.username(), null,
						account.role().permissions().stream().map(SimpleGrantedAuthority::new).toList());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}
		chain.doFilter(request, response);
	}

	private Account resolve(String header) {
		if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
			String token = header.substring(7).trim();
			if (token.isEmpty()) {
				return null;
			}
			return tokens.usernameFor(token).flatMap(accounts::find).orElse(null);
		}
		if (header.regionMatches(true, 0, "Basic ", 0, 6)) {
			try {
				String decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()),
						StandardCharsets.UTF_8);
				int colon = decoded.indexOf(':');
				if (colon < 0) {
					return null;
				}
				return accounts.authenticate(decoded.substring(0, colon), decoded.substring(colon + 1)).orElse(null);
			} catch (IllegalArgumentException notBase64) {
				return null;
			}
		}
		return null;
	}
}
