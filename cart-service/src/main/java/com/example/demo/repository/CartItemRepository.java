package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.CartItem;

public interface CartItemRepository
        extends JpaRepository<CartItem, Integer> {

    Optional<CartItem> findByUserIdAndProductId(
            Integer userId,
            Integer productId
    );

    List<CartItem> findByUserId(Integer userId);

    void deleteByUserIdAndProductId(
            Integer userId,
            Integer productId
    );

    void deleteByUserId(Integer userId);

    long countByUserId(Integer userId);
}