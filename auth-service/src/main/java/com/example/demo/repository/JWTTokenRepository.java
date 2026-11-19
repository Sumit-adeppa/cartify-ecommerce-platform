package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.JWTToken;

public interface JWTTokenRepository
        extends JpaRepository<JWTToken, Integer> {

    Optional<JWTToken> findByTokenHash(String tokenHash);

    void deleteByUserId(Integer userId);

    void deleteByTokenHash(String tokenHash);
}