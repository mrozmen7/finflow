package com.finflow.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration properties bound from application yml.
 *
 * @param secret     Base64-encoded 256-bit signing key
 * @param expiration Token lifetime in milliseconds (default: 86400000 = 24 h)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expiration) {}
