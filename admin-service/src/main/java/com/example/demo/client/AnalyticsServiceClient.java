package com.example.demo.client;

import com.example.demo.dto.AnalyticsSummaryResponse;
import com.example.demo.dto.DailySalesResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "analytics-service")
public interface AnalyticsServiceClient {

    @GetMapping("/api/analytics/summary")
    AnalyticsSummaryResponse getSummary(
            @RequestParam("from") LocalDate from,
            @RequestParam("to") LocalDate to
    );

    @GetMapping("/api/analytics/daily-sales")
    List<DailySalesResponse> getDailySales(
            @RequestParam("from") LocalDate from,
            @RequestParam("to") LocalDate to
    );
}