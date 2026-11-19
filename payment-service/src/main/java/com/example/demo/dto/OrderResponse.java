package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        String orderId,
        Integer userId,
        BigDecimal totalAmount,
        String status,
        LocalDateTime createdAt
) {}