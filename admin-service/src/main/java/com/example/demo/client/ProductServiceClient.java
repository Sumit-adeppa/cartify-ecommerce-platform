package com.example.demo.client;

import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @PostMapping("/api/products")
    ProductResponse addProduct(
            @RequestBody ProductRequest request
    );

    @DeleteMapping("/api/products/{productId}")
    void deleteProduct(
            @PathVariable("productId") Integer productId
    );
}