package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status
) {
    public enum OrderStatus {
        SUCCESS,
        FAILED
    }
}