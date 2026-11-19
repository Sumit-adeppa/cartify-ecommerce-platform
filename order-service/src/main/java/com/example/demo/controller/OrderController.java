package com.example.demo.controller;

import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.UpdateOrderStatusRequest;
import com.example.demo.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @PathVariable Integer userId) {

        return orderService.createOrder(userId);
    }

    @GetMapping("/user/{userId}")
    public List<OrderResponse> getOrdersForUser(
            @PathVariable Integer userId) {

        return orderService.getOrdersForUser(userId);
    }

    @GetMapping("/{orderId}/user/{userId}")
    public OrderResponse getOrderById(
            @PathVariable String orderId,
            @PathVariable Integer userId) {

        return orderService.getOrderById(orderId, userId);
    }

    @PutMapping("/{orderId}/user/{userId}/cancel")
    public OrderResponse cancelOrder(
            @PathVariable String orderId,
            @PathVariable Integer userId) {

        return orderService.cancelOrder(orderId, userId);
    }

    @PutMapping("/internal/{orderId}/payment-status")
    public OrderResponse updatePaymentStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {

        return orderService.updatePaymentStatus(
                orderId, request.status(), apiKey);
    }
}