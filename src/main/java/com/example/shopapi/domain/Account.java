package com.example.shopapi.domain;

/** An account with its password (plain text on purpose: this is a local test target). Never sent as JSON. */
public record Account(int id, String username, String password, String email, Role role, String createdAt) {
}
