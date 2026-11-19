package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(

        Integer userId,
        String username,
        List<CartItemResponse> products,
        Integer totalItems,
        BigDecimal overallTotalPrice

) {}