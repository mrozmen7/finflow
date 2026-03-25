package com.finflow.shared.security;

import com.finflow.shared.config.JwtProperties;
import com.finflow.shared.security.dto.LoginRequest;
import com.finflow.shared.security.dto.LoginResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoint.
 * TODO: Replace hardcoded credentials with database-backed user lookup once the User entity is implemented.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // TODO: Replace with database-backed user service
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthController(JwtService jwtService, JwtProperties jwtProperties) {
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Authenticates a user and returns a signed JWT token.
     *
     * @param request login credentials
     * @return 200 with JWT token on success, 401 on invalid credentials
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.username());

        if (!ADMIN_USERNAME.equals(request.username()) || !ADMIN_PASSWORD.equals(request.password())) {
            log.warn("Failed login attempt for user: {}", request.username());
            return ResponseEntity.status(401).build();
        }

        String token = jwtService.generateToken(request.username());
        log.info("Login successful for user: {}", request.username());

        return ResponseEntity.ok(LoginResponse.of(token, jwtProperties.expiration()));
    }
}
