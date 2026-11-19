package com.example.demo.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Integer userId,
        String username,
        String email,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}