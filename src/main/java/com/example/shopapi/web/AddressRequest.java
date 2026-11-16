package com.example.shopapi.web;

import com.example.shopapi.domain.Address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The "Address" schema. */
public record AddressRequest(
		@NotBlank @Size(max = 100) String street,
		@NotBlank @Size(max = 60) String city,
		@NotNull @Pattern(regexp = "[A-Za-z0-9 -]{3,10}") String postalCode,
		@NotNull @Pattern(regexp = "[A-Z]{2}") String country) {

	public Address toAddress() {
		return new Address(street, city, postalCode, country);
	}
}
