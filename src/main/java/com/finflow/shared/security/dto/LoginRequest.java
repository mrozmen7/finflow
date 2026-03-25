package com.finflow.shared.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the login endpoint.
 *
 * @param username the account username
 * @param password the account password
 */
public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
