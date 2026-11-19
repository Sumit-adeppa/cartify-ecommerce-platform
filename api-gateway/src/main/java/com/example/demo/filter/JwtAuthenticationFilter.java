package com.example.demo.filter;

import com.example.demo.dto.AuthValidationResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthenticationFilter implements GlobalFilter {

    private static final String AUTH_COOKIE = "authToken";

    private static final String USER_ID_HEADER =
            "X-Authenticated-User-Id";

    private static final String USERNAME_HEADER =
            "X-Authenticated-Username";

    private static final String ROLE_HEADER =
            "X-Authenticated-Role";

    private final WebClient webClient;

    public JwtAuthenticationFilter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path = exchange.getRequest()
                .getURI()
                .getPath();

        HttpMethod method = exchange.getRequest().getMethod();

        // Remove client-supplied identity headers.
        ServerHttpRequest sanitizedRequest = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USERNAME_HEADER);
                    headers.remove(ROLE_HEADER);
                })
                .build();

        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(sanitizedRequest)
                .build();

        // Block internal endpoints from public Gateway access.
        if (isInternalPath(path)) {
            return reject(sanitizedExchange, HttpStatus.NOT_FOUND);
        }

        // Allow public endpoints without JWT.
        if (isPublicEndpoint(path, method)) {
            return chain.filter(sanitizedExchange);
        }

        // Read the authentication cookie.
        HttpCookie authCookie = sanitizedRequest.getCookies()
                .getFirst(AUTH_COOKIE);

        if (authCookie == null || authCookie.getValue().isBlank()) {
            return reject(sanitizedExchange, HttpStatus.UNAUTHORIZED);
        }

        // Validate the token using Auth Service.
        return webClient.post()
                .uri("http://AUTH-SERVICE/api/auth/validate")
                .cookie(AUTH_COOKIE, authCookie.getValue())
                .exchangeToMono(response -> {

                    if (!response.statusCode().is2xxSuccessful()) {
                        return Mono.error(
                                new InvalidAuthenticationException()
                        );
                    }

                    return response.bodyToMono(
                            AuthValidationResponse.class
                    ).switchIfEmpty(
                            Mono.error(
                                    new InvalidAuthenticationException()
                            )
                    );
                })
                .flatMap(auth -> {

                    if (!auth.valid()
                            || auth.userId() == null
                            || auth.role() == null) {

                        return reject(
                                sanitizedExchange,
                                HttpStatus.UNAUTHORIZED
                        );
                    }

                    String role = auth.role().toUpperCase();

                    // Check role-based access.
                    if (!hasRequiredRole(path, method, role)) {
                        return reject(
                                sanitizedExchange,
                                HttpStatus.FORBIDDEN
                        );
                    }

                    // Check customer ownership.
                    if (!hasOwnershipAccess(
                            path,
                            method,
                            auth.userId(),
                            role)) {

                        return reject(
                                sanitizedExchange,
                                HttpStatus.FORBIDDEN
                        );
                    }

                    // Add trusted identity headers.
                    ServerHttpRequest authenticatedRequest =
                            sanitizedRequest.mutate()
                                    .headers(headers -> {
                                        headers.set(
                                                USER_ID_HEADER,
                                                auth.userId().toString()
                                        );

                                        headers.set(
                                                USERNAME_HEADER,
                                                auth.username()
                                        );

                                        headers.set(
                                                ROLE_HEADER,
                                                role
                                        );
                                    })
                                    .build();

                    return chain.filter(
                            sanitizedExchange.mutate()
                                    .request(authenticatedRequest)
                                    .build()
                    );
                })
                .onErrorResume(
                        InvalidAuthenticationException.class,
                        exception -> reject(
                                sanitizedExchange,
                                HttpStatus.UNAUTHORIZED
                        )
                )
                .onErrorResume(
                        WebClientRequestException.class,
                        exception -> reject(
                                sanitizedExchange,
                                HttpStatus.SERVICE_UNAVAILABLE
                        )
                );
    }

    private boolean isPublicEndpoint(
            String path,
            HttpMethod method) {

        if (method == HttpMethod.OPTIONS) {
            return true;
        }

        if (path.equals("/api/auth/login")
                || path.equals("/api/auth/logout")
                || path.equals("/api/auth/validate")) {
            return true;
        }

        // Public customer registration.
        if (method == HttpMethod.POST
                && path.equals("/api/users/register")) {
            return true;
        }

        // Public product and category browsing.
        if (method == HttpMethod.GET
                && (path.startsWith("/api/products/")
                || path.equals("/api/products")
                || path.startsWith("/api/categories/")
                || path.equals("/api/categories"))) {
            return true;
        }

        return false;
    }

    private boolean isInternalPath(String path) {

        return path.startsWith("/internal/")
                || path.contains("/internal/");
    }

    private boolean hasRequiredRole(
            String path,
            HttpMethod method,
            String role) {

        if (!"ADMIN".equals(role) && !"CUSTOMER".equals(role)) {
            return false;
        }

        // Admin-only endpoints.
        if (path.startsWith("/admin/")
                || path.startsWith("/api/analytics/")) {
            return "ADMIN".equals(role);
        }

        // Only administrators can create, update, or delete products/categories.
        if ((path.startsWith("/api/products")
                || path.startsWith("/api/categories"))
                && method != HttpMethod.GET) {
            return "ADMIN".equals(role);
        }

        return true;
    }

    private boolean hasOwnershipAccess(
            String path,
            HttpMethod method,
            Integer authenticatedUserId,
            String role) {

        // Administrators can access customer resources.
        if ("ADMIN".equals(role)) {
            return true;
        }

        Integer requestedUserId = extractUserId(path, method);

        // No user-specific path parameter.
        if (requestedUserId == null) {
            return true;
        }

        return requestedUserId.equals(authenticatedUserId);
    }

    private Integer extractUserId(
            String path,
            HttpMethod method) {

        List<String> parts = Arrays.asList(
                path.replaceFirst("^/", "").split("/")
        );

        try {

            // /api/users/{userId}
            if (parts.size() >= 3
                    && parts.get(0).equals("api")
                    && parts.get(1).equals("users")) {

                return Integer.valueOf(parts.get(2));
            }

            // /api/cart/{userId}/...
            if (parts.size() >= 3
                    && parts.get(0).equals("api")
                    && parts.get(1).equals("cart")) {

                return Integer.valueOf(parts.get(2));
            }

            // /api/orders/{userId}
            // POST /api/orders/{userId}
            if (parts.size() == 3
                    && parts.get(0).equals("api")
                    && parts.get(1).equals("orders")
                    && method == HttpMethod.POST) {

                return Integer.valueOf(parts.get(2));
            }

            // /api/orders/user/{userId}
            if (parts.size() >= 4
                    && parts.get(0).equals("api")
                    && parts.get(1).equals("orders")
                    && parts.get(2).equals("user")) {

                return Integer.valueOf(parts.get(3));
            }

            // /api/orders/{orderId}/user/{userId}
            if (parts.size() >= 5
                    && parts.get(0).equals("api")
                    && parts.get(1).equals("orders")
                    && parts.get(3).equals("user")) {

                return Integer.valueOf(parts.get(4));
            }

            // /api/payment/verify/{userId}
            if (parts.size() >= 4
                    && parts.get(0).equals("api")
                    && parts.get(1).equals("payment")
                    && parts.get(2).equals("verify")) {

                return Integer.valueOf(parts.get(3));
            }

            // /api/payment/create/{orderId}/user/{userId}
            if (parts.size() >= 6
                    && parts.get(0).equals("api")
                    && parts.get(1).equals("payment")
                    && parts.get(2).equals("create")
                    && parts.get(4).equals("user")) {

                return Integer.valueOf(parts.get(5));
            }

            // /api/payment/{razorpayOrderId}/user/{userId}
            if (parts.size() >= 5
                    && parts.get(0).equals("api")
                    && parts.get(1).equals("payment")
                    && parts.get(3).equals("user")) {

                return Integer.valueOf(parts.get(4));
            }

        } catch (NumberFormatException exception) {
            return -1;
        }

        return null;
    }

    private Mono<Void> reject(
            ServerWebExchange exchange,
            HttpStatus status) {

        exchange.getResponse().setStatusCode(status);

        return exchange.getResponse().setComplete();
    }

    private static class InvalidAuthenticationException
            extends RuntimeException {
    }
}
