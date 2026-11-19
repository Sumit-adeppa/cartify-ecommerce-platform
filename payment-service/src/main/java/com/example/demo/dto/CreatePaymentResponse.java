package com.example.demo.dto;

public record CreatePaymentResponse(
        String cartifyOrderId,
        String razorpayOrderId,
        String razorpayKeyId,
        Long amount,
        String currency,
        String status
) {}