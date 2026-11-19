package com.example.demo.controller;

import com.example.demo.dto.CreatePaymentResponse;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.dto.VerifyPaymentRequest;
import com.example.demo.service.PaymentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Create Razorpay order for an existing Cartify order.
    @PostMapping("/create/{orderId}/user/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentResponse createPayment(
            @PathVariable String orderId,
            @PathVariable Integer userId) {

        return paymentService.createPayment(orderId, userId);
    }

    // Verify Razorpay payment.
    @PostMapping("/verify/{userId}")
    public PaymentResponse verifyPayment(
            @PathVariable Integer userId,
            @Valid @RequestBody VerifyPaymentRequest request) {

        return paymentService.verifyPayment(userId, request);
    }

    // Get payment details.
    @GetMapping("/{razorpayOrderId}/user/{userId}")
    public PaymentResponse getPayment(
            @PathVariable String razorpayOrderId,
            @PathVariable Integer userId) {

        return paymentService.getPayment(razorpayOrderId, userId);
    }
}