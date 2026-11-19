package com.example.demo.controller;

import com.example.demo.repository.JWTTokenRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final JWTTokenRepository jwtTokenRepository;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public InternalAuthController(
            JWTTokenRepository jwtTokenRepository) {
        this.jwtTokenRepository = jwtTokenRepository;
    }

    @DeleteMapping("/users/{userId}/tokens")
    public Map<String, String> revokeUserTokens(
            @PathVariable Integer userId,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {

        if (internalApiKey == null
                || !internalApiKey.equals(apiKey)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid internal API key");
        }

        jwtTokenRepository.deleteByUserId(userId);

        return Map.of(
                "message",
                "User sessions revoked successfully"
        );
    }
}