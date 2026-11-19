package com.example.demo.dto;

import com.example.demo.entity.Role;

public record UserAuthDetails(
        Integer userId,
        String username,
        String email,
        String passwordHash,
        Role role
) {}