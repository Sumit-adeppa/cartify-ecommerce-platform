package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(

        @Size(min = 3, max = 50)
        String username,

        @Email
        @Size(max = 150)
        String email,

        String role
) {}