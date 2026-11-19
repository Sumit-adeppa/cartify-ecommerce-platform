package com.example.demo.service;

import org.springframework.transaction.annotation.Transactional;

import com.example.demo.client.CartServiceClient;
import com.example.demo.client.ProductServiceClient;
import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.OrderRepository;

import feign.FeignException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartServiceClient cartServiceClient;
    private final ProductServiceClient productServiceClient;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public OrderService(
            OrderRepository orderRepository,
            CartServiceClient cartServiceClient,
            ProductServiceClient productServiceClient) {

        this.orderRepository = orderRepository;
        this.cartServiceClient = cartServiceClient;
        this.productServiceClient = productServiceClient;
    }

    public OrderResponse createOrder(Integer userId) {

        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid user ID");
        }

        CartResponse cart;

        try {
            cart = cartServiceClient.getCart(userId);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User or cart not found");
        } catch (FeignException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cart Service is unavailable");
        }

        if (cart == null || cart.products() == null
                || cart.products().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Your cart is empty");
        }

        if (!userId.equals(cart.userId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cart user does not match");
        }

        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItemResponse cartItem : cart.products()) {

            ProductResponse product;

            try {
                product = productServiceClient.getProduct(
                        cartItem.productId());
            } catch (FeignException.NotFound ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Product no longer exists: " + cartItem.productId());
            } catch (FeignException ex) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Product Service is unavailable");
            }

            if (product == null || product.stock() == null
                    || product.price() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid product information");
            }

            if (cartItem.quantity() == null || cartItem.quantity() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid quantity for product: " + product.name());
            }

            if (product.stock() < cartItem.quantity()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Insufficient stock for product: " + product.name());
            }

            // Use the current Product Service price.
            BigDecimal unitPrice = product.price();

            BigDecimal lineTotal = unitPrice.multiply(
                    BigDecimal.valueOf(cartItem.quantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.productId());
            orderItem.setProductName(product.name());
            orderItem.setImageUrl(
                    product.images() == null
                            || product.images().isEmpty()
                            ? null : product.images().get(0));
            orderItem.setQuantity(cartItem.quantity());
            orderItem.setPricePerUnit(unitPrice);
            orderItem.setTotalPrice(lineTotal);

            order.addOrderItem(orderItem);
            totalAmount = totalAmount.add(lineTotal);
        }

        order.setTotalAmount(totalAmount);

        // Save the order before clearing the cart.
        Order savedOrder = orderRepository.save(order);

        try {
            cartServiceClient.clearCart(userId);
        } catch (FeignException ex) {
            // The order has already been saved.
            // Do not create another order automatically.
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Order was created, but the cart could not be cleared. "
                            + "Order ID: " + savedOrder.getOrderId());
        }

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(Integer userId) {

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(
            String orderId, Integer userId) {

        Order order = orderRepository
                .findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));

        return mapToResponse(order);
    }

    public OrderResponse cancelOrder(
            String orderId, Integer userId) {

        Order order = orderRepository
                .findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending orders can be cancelled");
        }

        // FAILED is retained to match the original Cartify enum.
        order.setStatus(OrderStatus.FAILED);

        return mapToResponse(orderRepository.save(order));
    }

    public OrderResponse updatePaymentStatus(
            String orderId,
            OrderStatus newStatus,
            String suppliedApiKey) {

        if (suppliedApiKey == null
                || !internalApiKey.equals(suppliedApiKey)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid internal API key");
        }

        if (newStatus != OrderStatus.SUCCESS
                && newStatus != OrderStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment can update status to SUCCESS or FAILED only");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));

        // If the requested status is already applied, return successfully.
        // This makes payment-status updates safe to retry.
        if (order.getStatus() == newStatus) {
            return mapToResponse(order);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending orders can receive a payment update");
        }

        order.setStatus(newStatus);

        return mapToResponse(orderRepository.save(order));
    }

    private OrderResponse mapToResponse(Order order) {

        List<OrderItemResponse> products =
                order.getOrderItems().stream()
                        .map(item -> new OrderItemResponse(
                                item.getProductId(),
                                item.getProductName(),
                                item.getImageUrl(),
                                item.getQuantity(),
                                item.getPricePerUnit(),
                                item.getTotalPrice()
                        ))
                        .toList();

        return new OrderResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                products
        );
    }
}