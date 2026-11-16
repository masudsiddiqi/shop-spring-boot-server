package com.example.shopapi.domain;

/** The "Address" schema of the contract. */
public record Address(String street, String city, String postalCode, String country) {
}
