package com.example.demo.dto;

import java.math.BigDecimal;

public record CartItemResponse(

        Integer productId,
        String name,
        String description,
        String imageUrl,
        BigDecimal pricePerUnit,
        Integer quantity,
        BigDecimal totalPrice

) {}