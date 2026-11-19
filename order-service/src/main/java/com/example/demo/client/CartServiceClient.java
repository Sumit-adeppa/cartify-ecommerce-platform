package com.example.demo.client;

import com.example.demo.dto.CartResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cart-service")
public interface CartServiceClient {

    @GetMapping("/api/cart/{userId}")
    CartResponse getCart(@PathVariable("userId") Integer userId);

    @DeleteMapping("/api/cart/{userId}/clear")
    void clearCart(@PathVariable("userId") Integer userId);
}