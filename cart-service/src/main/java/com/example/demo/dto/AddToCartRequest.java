package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddToCartRequest(

        @NotNull(message = "Product ID is required")
        @Positive
        Integer productId,

        @NotNull(message = "Quantity is required")
        @Positive
        Integer quantity

) {}