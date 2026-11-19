package com.example.demo.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AdminProductRequest(

        @NotBlank
        String name,

        @NotBlank
        String description,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal price,

        @NotNull
        @PositiveOrZero
        Integer stock,

        @NotNull
        @Positive
        Integer categoryId,

        @NotBlank
        String imageUrl
) {}