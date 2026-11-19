package com.example.demo.dto;

import java.math.BigDecimal;

public record AnalyticsSummaryResponse(
        long totalOrders,
        long successfulOrders,
        long pendingOrders,
        long failedOrders,
        BigDecimal totalRevenue
) {}