package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.*;

public record ProductRequest(

        @NotBlank(message = "Product name is required")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01")
        BigDecimal price,

        @NotNull(message = "Stock is required")
        @PositiveOrZero
        Integer stock,

        @NotNull(message = "Category ID is required")
        Integer categoryId,

        List<@NotBlank String> imageUrls

) {}