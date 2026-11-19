package com.example.demo.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.AddToCartRequest;
import com.example.demo.dto.CartResponse;
import com.example.demo.dto.UpdateCartRequest;
import com.example.demo.service.CartService;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // Add product to cart
    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponse> addToCart(
            @PathVariable Integer userId,
            @Valid @RequestBody AddToCartRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addToCart(userId, request));
    }

    // Get cart
    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(
            @PathVariable Integer userId) {

        return ResponseEntity.ok(
                cartService.getCart(userId)
        );
    }

    // Update quantity
    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @PathVariable Integer userId,
            @PathVariable Integer productId,
            @Valid @RequestBody UpdateCartRequest request) {

        return ResponseEntity.ok(
                cartService.updateQuantity(
                        userId,
                        productId,
                        request
                )
        );
    }

    // Remove product
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<Map<String, String>> removeItem(
            @PathVariable Integer userId,
            @PathVariable Integer productId) {

        cartService.removeItem(userId, productId);

        return ResponseEntity.ok(
                Map.of("message", "Product removed from cart")
        );
    }

    // Clear cart
    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Map<String, String>> clearCart(
            @PathVariable Integer userId) {

        cartService.clearCart(userId);

        return ResponseEntity.ok(
                Map.of("message", "Cart cleared successfully")
        );
    }

    // Get total quantity
    @GetMapping("/{userId}/count")
    public ResponseEntity<Map<String, Integer>> getCartItemCount(
            @PathVariable Integer userId) {

        return ResponseEntity.ok(
                Map.of(
                        "totalItems",
                        cartService.getCartItemCount(userId)
                )
        );
    }
}