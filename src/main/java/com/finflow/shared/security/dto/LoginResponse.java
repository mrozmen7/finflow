package com.finflow.shared.security.dto;

/**
 * Response body returned after a successful login.
 *
 * @param token      the signed JWT bearer token
 * @param tokenType  always {@code "Bearer"}
 * @param expiresIn  token lifetime in seconds
 */
public record LoginResponse(String token, String tokenType, long expiresIn) {

    public static LoginResponse of(String token, long expirationMs) {
        return new LoginResponse(token, "Bearer", expirationMs / 1000);
    }
}
