
package com.example.demo.service;

import com.example.demo.client.OrderAnalyticsClient;
import com.example.demo.dto.*;

import feign.FeignException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

@Service
public class AnalyticsService {

    private static final Logger log =
            LoggerFactory.getLogger(AnalyticsService.class);

    private final OrderAnalyticsClient orderAnalyticsClient;
    private final String internalApiKey;

    public AnalyticsService(
            OrderAnalyticsClient orderAnalyticsClient,
            @Value("${internal.api-key}") String internalApiKey) {

        this.orderAnalyticsClient = orderAnalyticsClient;
        this.internalApiKey = internalApiKey;
    }

    // Validate date range
    private void validateDates(LocalDate from, LocalDate to) {

        if (from == null || to == null || from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid date range"
            );
        }
    }

    // Centralized Order Service exception handling
    private <T> T executeOrderServiceRequest(
            Supplier<T> request,
            String errorMessage) {

        try {
            return request.get();

        } catch (FeignException ex) {

            int status = ex.status();

            log.error(
                    "{}. Order Service returned HTTP status: {}",
                    errorMessage,
                    status,
                    ex
            );

            // Connection failure or no HTTP response
            if (status == -1) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Order Service is unavailable"
                );
            }

            // Order Service returned an unsuccessful response
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    errorMessage + ". Order Service returned status: " + status
            );
        }
    }

    // Analytics Summary
    public AnalyticsSummaryResponse getSummary(
            LocalDate from,
            LocalDate to) {

        validateDates(from, to);

        return executeOrderServiceRequest(
                () -> orderAnalyticsClient.getSummary(
                        from,
                        to,
                        internalApiKey
                ),
                "Unable to retrieve analytics summary"
        );
    }

    // Daily Sales
    public List<DailySalesResponse> getDailySales(
            LocalDate from,
            LocalDate to) {

        validateDates(from, to);

        return executeOrderServiceRequest(
                () -> orderAnalyticsClient.getDailySales(
                        from,
                        to,
                        internalApiKey
                ),
                "Unable to retrieve daily sales"
        );
    }

    // Top Products
    public List<TopProductResponse> getTopProducts(
            LocalDate from,
            LocalDate to,
            int limit) {

        validateDates(from, to);

        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Limit must be between 1 and 100"
            );
        }

        return executeOrderServiceRequest(
                () -> orderAnalyticsClient.getTopProducts(
                        from,
                        to,
                        limit,
                        internalApiKey
                ),
                "Unable to retrieve top products"
        );
    }
}
