package com.example.demo.client;

import com.example.demo.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/api/products/{productId}")
    ProductResponse getProduct(
            @PathVariable("productId") Integer productId
    );
}