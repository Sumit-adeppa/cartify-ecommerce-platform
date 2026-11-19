package com.example.demo.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Integer productId,
        String productName,
        String imageUrl,
        Integer quantity,
        BigDecimal pricePerUnit,
        BigDecimal totalPrice
) {}