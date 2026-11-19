package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.OrderStatus;
import com.example.demo.repository.OrderRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderAnalyticsService {

    private final OrderRepository orderRepository;

    public OrderAnalyticsService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    private LocalDateTime startOf(LocalDate date) {
        return date.atStartOfDay();
    }

    // End date is inclusive.
    private LocalDateTime endExclusive(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }

    public AnalyticsSummaryResponse getSummary(
            LocalDate from, LocalDate to) {

        LocalDateTime start = startOf(from);
        LocalDateTime end = endExclusive(to);

        long total = orderRepository
                .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        start, end);

        long success = orderRepository
                .countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        OrderStatus.SUCCESS, start, end);

        long pending = orderRepository
                .countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        OrderStatus.PENDING, start, end);

        long failed = orderRepository
                .countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        OrderStatus.FAILED, start, end);

        BigDecimal revenue = orderRepository.getRevenue(
                OrderStatus.SUCCESS, start, end);

        return new AnalyticsSummaryResponse(
                total,
                success,
                pending,
                failed,
                revenue == null ? BigDecimal.ZERO : revenue
        );
    }

    public List<DailySalesResponse> getDailySales(
            LocalDate from, LocalDate to) {

        List<Object[]> rows = orderRepository.getDailySales(
                startOf(from), endExclusive(to));

        return rows.stream()
                .map(row -> new DailySalesResponse(
                        ((Date) row[0]).toLocalDate(),
                        ((Number) row[1]).longValue(),
                        (BigDecimal) row[2]
                ))
                .toList();
    }

    public List<TopProductResponse> getTopProducts(
            LocalDate from, LocalDate to, int limit) {

        List<Object[]> rows = orderRepository.getTopProducts(
                startOf(from), endExclusive(to), limit);

        return rows.stream()
                .map(row -> new TopProductResponse(
                        ((Number) row[0]).intValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        (BigDecimal) row[3]
                ))
                .toList();
    }
}