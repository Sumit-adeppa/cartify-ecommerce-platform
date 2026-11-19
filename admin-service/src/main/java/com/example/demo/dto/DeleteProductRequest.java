package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DeleteProductRequest(
        @NotNull
        @Positive
        Integer productId
) {}