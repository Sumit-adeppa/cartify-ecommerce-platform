package com.example.demo.dto;

public record TokenValidationResponse(
        boolean valid,
        Integer userId,
        String username,
        String role
) {}