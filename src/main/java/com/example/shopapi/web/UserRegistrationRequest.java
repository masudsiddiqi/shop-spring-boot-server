package com.example.shopapi.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The "UserRegistration" schema (POST /users). A missing field or a rule breach is a 422. */
public record UserRegistrationRequest(
		@NotNull @Size(min = 3, max = 30) @Pattern(regexp = "[A-Za-z0-9_]+") String username,
		@NotNull @Size(min = 8, max = 72) String password,
		@NotNull @Email String email) {
}
