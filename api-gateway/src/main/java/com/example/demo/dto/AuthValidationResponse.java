package com.example.demo.dto;

public record AuthValidationResponse(
        boolean valid,
        Integer userId,
        String username,
        String role
) {
}
