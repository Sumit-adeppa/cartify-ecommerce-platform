package com.example.demo.service;

import com.example.demo.client.*;
import com.example.demo.dto.*;

import feign.FeignException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class AdminService {

    private final UserServiceClient userClient;
    private final ProductServiceClient productClient;
    private final AnalyticsServiceClient analyticsClient;
    private final AuthServiceClient authClient;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public AdminService(
            UserServiceClient userClient,
            ProductServiceClient productClient,
            AnalyticsServiceClient analyticsClient,
            AuthServiceClient authClient) {

        this.userClient = userClient;
        this.productClient = productClient;
        this.analyticsClient = analyticsClient;
        this.authClient = authClient;
    }

    // ---------------- USER MANAGEMENT ----------------

    public UserResponse getUserById(Integer userId) {
        try {
            return userClient.getUserById(userId);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found");
        } catch (FeignException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "User Service is unavailable");
        }
    }

    public UserResponse modifyUser(AdminUserUpdateRequest request) {

        if (request.username() == null && request.email() == null
                && request.role() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide at least one field to update");
        }

        if (request.role() != null
                && !List.of("ADMIN", "CUSTOMER")
                .contains(request.role())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role must be ADMIN or CUSTOMER");
        }

        UserResponse existingUser = getUserById(request.userId());

        try {
            UserResponse updatedUser = userClient.updateUser(
                    request.userId(), request, internalApiKey);

            // A role change must invalidate the user's existing sessions.
            if (request.role() != null
                    && !request.role().equals(existingUser.role())) {
                try {
                    authClient.revokeUserTokens(
                            request.userId(), internalApiKey);
                } catch (FeignException ex) {
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "User was updated, but session revocation failed. "
                                    + "Revoke sessions before allowing access.");
                }
            }

            return updatedUser;

        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found");
        } catch (FeignException.Conflict ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username or email may already be in use");
        } catch (FeignException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to update user");
        }
    }

    // ---------------- PRODUCT MANAGEMENT ----------------

    public ProductResponse addProduct(AdminProductRequest request) {

        ProductRequest productRequest = new ProductRequest(
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.categoryId(),
                List.of(request.imageUrl())
        );

        try {
            return productClient.addProduct(productRequest);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid category ID");
        } catch (FeignException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to add product");
        }
    }

    public void deleteProduct(Integer productId) {

        try {
            productClient.deleteProduct(productId);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found");
        } catch (FeignException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to delete product");
        }
    }

    // ---------------- BUSINESS REPORTS ----------------

    public AnalyticsSummaryResponse getDailyBusiness(LocalDate date) {
        return getBusinessSummary(date, date);
    }

    public AnalyticsSummaryResponse getMonthlyBusiness(
            int month, int year) {

        if (month < 1 || month > 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid month");
        }

        if (year < 2000 || year > 2100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid year");
        }

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        return getBusinessSummary(from, to);
    }

    public AnalyticsSummaryResponse getYearlyBusiness(int year) {

        if (year < 2000 || year > 2100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid year");
        }

        return getBusinessSummary(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31));
    }

    public AnalyticsSummaryResponse getOverallBusiness() {
        return getBusinessSummary(
                LocalDate.of(2000, 1, 1),
                LocalDate.now());
    }

    private AnalyticsSummaryResponse getBusinessSummary(
            LocalDate from, LocalDate to) {

        try {
            return analyticsClient.getSummary(from, to);
        } catch (FeignException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Analytics Service is unavailable");
        }
    }

    public List<DailySalesResponse> getDailySales(
            LocalDate from, LocalDate to) {

        if (from == null || to == null || from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid date range");
        }

        try {
            return analyticsClient.getDailySales(from, to);
        } catch (FeignException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Analytics Service is unavailable");
        }
    }
}