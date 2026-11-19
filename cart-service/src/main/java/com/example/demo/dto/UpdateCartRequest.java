package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateCartRequest(

        @NotNull(message = "Quantity is required")
        @Positive
        Integer quantity

) {}