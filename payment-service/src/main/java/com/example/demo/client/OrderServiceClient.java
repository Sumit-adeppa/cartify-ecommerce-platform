package com.example.demo.client;

import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.UpdateOrderStatusRequest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/orders/{orderId}/user/{userId}")
    OrderResponse getOrder(
            @PathVariable("orderId") String orderId,
            @PathVariable("userId") Integer userId
    );

    @PutMapping("/api/orders/internal/{orderId}/payment-status")
    void updatePaymentStatus(
            @PathVariable("orderId") String orderId,
            @RequestBody UpdateOrderStatusRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    );
}