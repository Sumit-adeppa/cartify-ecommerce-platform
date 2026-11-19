package com.example.demo.dto;

import com.example.demo.entity.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentRecordId,
        String orderId,
        String razorpayOrderId,
        String razorpayPaymentId,
        Long amount,
        String currency,
        PaymentStatus status,
        LocalDateTime createdAt
) {}