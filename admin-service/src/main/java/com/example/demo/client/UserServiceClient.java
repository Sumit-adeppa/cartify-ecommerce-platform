package com.example.demo.client;

import com.example.demo.dto.UserResponse;
import com.example.demo.dto.AdminUserUpdateRequest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    UserResponse getUserById(
            @PathVariable("userId") Integer userId
    );

    @PutMapping("/internal/users/{userId}/admin-profile")
    UserResponse updateUser(
            @PathVariable("userId") Integer userId,
            @RequestBody AdminUserUpdateRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    );
}