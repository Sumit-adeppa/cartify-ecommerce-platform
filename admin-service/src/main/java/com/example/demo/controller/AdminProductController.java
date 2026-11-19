package com.example.demo.controller;

import com.example.demo.dto.AdminProductRequest;
import com.example.demo.dto.DeleteProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.service.AdminService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    private final AdminService adminService;

    public AdminProductController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/add")
    public ResponseEntity<ProductResponse> addProduct(
            @Valid @RequestBody AdminProductRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.addProduct(request));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteProduct(
            @Valid @RequestBody DeleteProductRequest request) {

        adminService.deleteProduct(request.productId());

        return ResponseEntity.ok(
                Map.of("message", "Product deleted successfully"));
    }
}