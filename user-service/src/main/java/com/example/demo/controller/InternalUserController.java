package com.example.demo.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.UserAuthDetails;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserRepository userRepository;
    private final String internalApiKey;

    public InternalUserController(
            UserRepository userRepository,
            @Value("${internal.api-key}") String internalApiKey) {

        this.userRepository = userRepository;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/by-username/{username}")
    public UserAuthDetails getUserForAuthentication(
            @PathVariable String username,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {

        if (!MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8))) {

            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Internal access denied"
            );
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new org.springframework.web.server.ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        return new UserAuthDetails(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole()
        );
    }
}