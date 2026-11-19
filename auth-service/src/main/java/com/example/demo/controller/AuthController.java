package com.example.demo.controller;

import java.time.Duration;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.TokenValidationResponse;
import com.example.demo.dto.UserAuthDetails;
import com.example.demo.service.AuthService;

import io.jsonwebtoken.Claims;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService,
            @Value("${cookie.secure:false}") boolean cookieSecure) {

        this.authService = authService;
        this.cookieSecure = cookieSecure;
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        UserAuthDetails user = authService.authenticate(
                request.username(),
                request.password()
        );

        String token = authService.generateToken(user);

        ResponseCookie cookie = createAuthCookie(token);

        LoginResponse response = new LoginResponse(
                "Login successful",
                user.username(),
                user.role().name()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    // Validate token
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @CookieValue(
                    name = "authToken",
                    required = false
            ) String token) {

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenValidationResponse(
                            false, null, null, null
                    ));
        }

        Claims claims = authService.validateToken(token);

        TokenValidationResponse response =
                new TokenValidationResponse(
                        true,
                        claims.get("userId", Integer.class),
                        claims.getSubject(),
                        claims.get("role", String.class)
                );

        return ResponseEntity.ok(response);
    }

    // Logout
    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(
            @CookieValue(
                    name = "authToken",
                    required = false
            ) String token) {

        authService.logout(token);

        ResponseCookie expiredCookie = createAuthCookie("")
                .mutate()
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredCookie.toString()
                )
                .body(new LoginResponse(
                        "Logout successful",
                        null,
                        null
                ));
    }

    private ResponseCookie createAuthCookie(String token) {

        return ResponseCookie.from("authToken", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(60))
                .build();
    }
}