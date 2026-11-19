package com.example.demo.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator cartifyRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .uri("lb://AUTH-SERVICE"))
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("lb://USER-SERVICE"))
                .route("product-service", r -> r.path("/api/products/**", "/api/categories/**")
                        .uri("lb://PRODUCT-SERVICE"))
                .route("cart-service", r -> r.path("/api/cart/**")
                        .uri("lb://CART-SERVICE"))
                .route("order-service", r -> r.path("/api/orders/**")
                        .uri("lb://ORDER-SERVICE"))
                // Payment controller uses /api/payment (singular).
                .route("payment-service", r -> r.path("/api/payment/**")
                        .uri("lb://PAYMENT-SERVICE"))
                // Admin controllers are mapped under /admin/**, not /api/admin/**.
                .route("admin-service", r -> r.path("/admin/**")
                        .uri("lb://ADMIN-SERVICE"))
                .route("analytics-service", r -> r.path("/api/analytics/**")
                        .uri("lb://ANALYTICS-SERVICE"))
                .build();
    }
}
