package com.example.demo.repository;

import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayOrderIdAndUserId(
            String razorpayOrderId,
            Integer userId
    );

    List<Payment> findByOrderIdOrderByCreatedAtDesc(String orderId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<Payment> findByStatus(PaymentStatus status);
}