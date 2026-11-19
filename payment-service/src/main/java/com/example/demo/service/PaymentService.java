package com.example.demo.service;

import com.example.demo.client.OrderServiceClient;
import com.example.demo.dto.*;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderServiceClient orderServiceClient) {

        this.paymentRepository = paymentRepository;
        this.orderServiceClient = orderServiceClient;
    }

    public CreatePaymentResponse createPayment(
            String orderId,
            Integer userId) {

        // 1. Fetch the Cartify order.
        OrderResponse order;

        try {
            order = orderServiceClient.getOrder(orderId, userId);
        } catch (feign.FeignException.NotFound ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Order not found");
        } catch (feign.FeignException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Order Service is unavailable");
        }

        if (order == null || !userId.equals(order.userId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Order not found");
        }

        if (!"PENDING".equals(order.status())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending orders can be paid");
        }

        if (order.totalAmount() == null
                || order.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid order amount");
        }

        // 2. Convert rupees to paise without rounding.
        long amountPaise;

        try {
            amountPaise = order.totalAmount()
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order amount must have no more than two decimal places");
        }

        // Razorpay expects the amount in the smallest currency unit.
        if (amountPaise > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order amount exceeds the supported limit");
        }

        // 3. Create Razorpay order.
        try {
            RazorpayClient razorpayClient =
                    new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject request = new JSONObject();
            request.put("amount", amountPaise);
            request.put("currency", "INR");
            request.put("receipt", orderId);

            com.razorpay.Order razorpayOrder =
                    razorpayClient.orders.create(request);

            String razorpayOrderId = razorpayOrder.get("id");

            // 4. Save payment record in Payment DB.
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setUserId(userId);
            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setAmount(amountPaise);
            payment.setCurrency("INR");
            payment.setStatus(PaymentStatus.PENDING);

            paymentRepository.save(payment);

            // 5. Return checkout details to frontend.
            return new CreatePaymentResponse(
                    orderId,
                    razorpayOrderId,
                    razorpayKeyId,
                    amountPaise,
                    "INR",
                    "PENDING"
            );

        } catch (RazorpayException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to create Razorpay order");
        }
    }

    public PaymentResponse verifyPayment(
            Integer userId,
            VerifyPaymentRequest request) {

        // 1. Find the payment record belonging to this user.
        Payment payment = paymentRepository
                .findByRazorpayOrderIdAndUserId(
                        request.razorpayOrderId(), userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment record not found"));

        // 2. Verify that the payment belongs to the expected order.
        if (payment.getStatus() == PaymentStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Payment has already failed");
        }

        if (payment.getRazorpayPaymentId() != null
                && !payment.getRazorpayPaymentId()
                .equals(request.razorpayPaymentId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment ID does not match the recorded payment");
        }

        // 3. Verify Razorpay signature.
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id",
                    request.razorpayOrderId());
            attributes.put("razorpay_payment_id",
                    request.razorpayPaymentId());
            attributes.put("razorpay_signature",
                    request.razorpaySignature());

            boolean validSignature = Utils.verifyPaymentSignature(
                    attributes, razorpayKeySecret);

            if (!validSignature) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment signature verification failed");
            }

        } catch (RazorpayException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to verify payment signature");
        }

        // 4. Save successful payment verification.
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setStatus(PaymentStatus.SUCCESS);

        Payment savedPayment = paymentRepository.save(payment);

        // 5. Notify Order Service.
        try {
            UpdateOrderStatusRequest statusRequest =
                    new UpdateOrderStatusRequest(
                            UpdateOrderStatusRequest.OrderStatus.SUCCESS);

            orderServiceClient.updatePaymentStatus(
                    payment.getOrderId(),
                    statusRequest,
                    internalApiKey
            );

        } catch (feign.FeignException ex) {
            // Payment is already verified and saved.
            // The Order Service update must be retried/reconciled.
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment verified, but Order Service could not be updated. "
                            + "Payment ID: " + savedPayment.getPaymentRecordId());
        }

        return mapToResponse(savedPayment);
    }

    public PaymentResponse getPayment(
            String razorpayOrderId,
            Integer userId) {

        Payment payment = paymentRepository
                .findByRazorpayOrderIdAndUserId(razorpayOrderId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found"));

        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {

        return new PaymentResponse(
                payment.getPaymentRecordId(),
                payment.getOrderId(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}