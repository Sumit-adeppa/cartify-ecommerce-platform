package com.example.demo.dto;

import java.math.BigDecimal;

public record TopProductResponse(
        Integer productId,
        String productName,
        long quantitySold,
        BigDecimal revenue
) {}