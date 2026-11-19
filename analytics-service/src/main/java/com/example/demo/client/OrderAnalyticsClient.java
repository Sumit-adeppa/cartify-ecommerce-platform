
package com.example.demo.client;

import com.example.demo.dto.*;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "order-service")
public interface OrderAnalyticsClient {

    @GetMapping("/internal/analytics/summary")
    AnalyticsSummaryResponse getSummary(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestHeader("X-Internal-Api-Key") String apiKey
    );

    @GetMapping("/internal/analytics/daily-sales")
    List<DailySalesResponse> getDailySales(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestHeader("X-Internal-Api-Key") String apiKey
    );

    @GetMapping("/internal/analytics/top-products")
    List<TopProductResponse> getTopProducts(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam("limit") int limit,

            @RequestHeader("X-Internal-Api-Key") String apiKey
    );
}
