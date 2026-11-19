package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.OrderAnalyticsService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/internal/analytics")
public class OrderAnalyticsController {

    private final OrderAnalyticsService analyticsService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public OrderAnalyticsController(
            OrderAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    private void verifyApiKey(String suppliedKey) {
        if (internalApiKey == null
                || internalApiKey.isBlank()
                || suppliedKey == null
                || !internalApiKey.equals(suppliedKey)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid internal API key");
        }
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid date range");
        }
    }

    @GetMapping("/summary")
    public AnalyticsSummaryResponse getSummary(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestHeader("X-Internal-Api-Key") String apiKey) {

        verifyApiKey(apiKey);
        validateDates(from, to);

        return analyticsService.getSummary(from, to);
    }

    @GetMapping("/daily-sales")
    public List<DailySalesResponse> getDailySales(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestHeader("X-Internal-Api-Key") String apiKey) {

        verifyApiKey(apiKey);
        validateDates(from, to);

        return analyticsService.getDailySales(from, to);
    }

    @GetMapping("/top-products")
    public List<TopProductResponse> getTopProducts(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(defaultValue = "10") int limit,

            @RequestHeader("X-Internal-Api-Key") String apiKey) {

        verifyApiKey(apiKey);
        validateDates(from, to);

        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Limit must be between 1 and 100");
        }

        return analyticsService.getTopProducts(from, to, limit);
    }
}