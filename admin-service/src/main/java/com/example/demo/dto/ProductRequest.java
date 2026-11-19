package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Integer categoryId,
        List<String> imageUrls
) {}