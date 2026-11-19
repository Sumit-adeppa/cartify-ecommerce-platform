package com.example.demo.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.client.ProductServiceClient;
import com.example.demo.client.UserServiceClient;
import com.example.demo.dto.*;
import com.example.demo.entity.CartItem;
import com.example.demo.repository.CartItemRepository;

@Service
public class CartService {

    private final CartItemRepository cartRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    public CartService(
            CartItemRepository cartRepository,
            UserServiceClient userServiceClient,
            ProductServiceClient productServiceClient) {

        this.cartRepository = cartRepository;
        this.userServiceClient = userServiceClient;
        this.productServiceClient = productServiceClient;
    }

    // Add product to cart
    @Transactional
    public CartResponse addToCart(
            Integer userId,
            AddToCartRequest request) {

        validateUser(userId);

        ProductResponse product =
                getProduct(request.productId());

        CartItem existingItem = cartRepository
                .findByUserIdAndProductId(
                        userId,
                        request.productId()
                )
                .orElse(null);

        int newQuantity = request.quantity();

        if (existingItem != null) {
            newQuantity += existingItem.getQuantity();
        }

        validateStock(product, newQuantity);

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
            cartRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem(
                    userId,
                    request.productId(),
                    newQuantity
            );

            cartRepository.save(newItem);
        }

        return getCart(userId);
    }

    // Get complete cart
    @Transactional(readOnly = true)
    public CartResponse getCart(Integer userId) {

        UserResponse user = validateUser(userId);

        List<CartItem> cartItems =
                cartRepository.findByUserId(userId);

        List<CartItemResponse> products = cartItems.stream()
                .map(this::toCartItemResponse)
                .toList();

        BigDecimal overallTotal = products.stream()
                .map(CartItemResponse::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = products.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        return new CartResponse(
                user.userId(),
                user.username(),
                products,
                totalItems,
                overallTotal
        );
    }

    // Update product quantity
    @Transactional
    public CartResponse updateQuantity(
            Integer userId,
            Integer productId,
            UpdateCartRequest request) {

        validateUser(userId);

        ProductResponse product = getProduct(productId);

        CartItem cartItem = cartRepository
                .findByUserIdAndProductId(userId, productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product is not present in cart"
                        )
                );

        validateStock(product, request.quantity());

        cartItem.setQuantity(request.quantity());

        cartRepository.save(cartItem);

        return getCart(userId);
    }

    // Remove product from cart
    @Transactional
    public void removeItem(Integer userId, Integer productId) {

        validateUser(userId);

        if (cartRepository.findByUserIdAndProductId(
                userId, productId).isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product is not present in cart"
            );
        }

        cartRepository.deleteByUserIdAndProductId(
                userId,
                productId
        );
    }

    // Clear complete cart
    @Transactional
    public void clearCart(Integer userId) {

        validateUser(userId);

        cartRepository.deleteByUserId(userId);
    }

    // Get total quantity of cart products
    @Transactional(readOnly = true)
    public Integer getCartItemCount(Integer userId) {

        validateUser(userId);

        return cartRepository.findByUserId(userId)
                .stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    // Validate user through User Service
    private UserResponse validateUser(Integer userId) {

        try {
            return userServiceClient.getUserById(userId);

        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found or User Service unavailable"
            );
        }
    }

    // Get product through Product Service
    private ProductResponse getProduct(Integer productId) {

        try {
            ProductResponse product =
                    productServiceClient.getProductById(productId);

            if (product == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                );
            }

            return product;

        } catch (ResponseStatusException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Product Service is unavailable"
            );
        }
    }

    // Check available stock
    private void validateStock(
            ProductResponse product,
            Integer requestedQuantity) {

        if (product.stock() == null
                || requestedQuantity > product.stock()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Requested quantity exceeds available stock"
            );
        }
    }

    // Convert cart item to response DTO
    private CartItemResponse toCartItemResponse(CartItem item) {

        ProductResponse product = getProduct(item.getProductId());

        String imageUrl = null;

        if (product.images() != null && !product.images().isEmpty()) {
            imageUrl = product.images().get(0);
        }

        BigDecimal totalPrice = product.price()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CartItemResponse(
                product.productId(),
                product.name(),
                product.description(),
                imageUrl,
                product.price(),
                item.getQuantity(),
                totalPrice
        );
    }
}