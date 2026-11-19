package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(

        Integer productId,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Integer categoryId,
        String categoryName,
        List<String> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}