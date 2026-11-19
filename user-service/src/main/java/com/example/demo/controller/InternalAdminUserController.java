
package com.example.demo.controller;

import com.example.demo.dto.AdminUserUpdateRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/users")
public class InternalAdminUserController {

    private final UserService userService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public InternalAdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/{userId}/admin-profile")
    public UserResponse updateUser(
            @PathVariable Integer userId,

            @Valid
            @RequestBody AdminUserUpdateRequest request,

            @RequestHeader("X-Internal-Api-Key") String apiKey) {

        if (internalApiKey == null
                || internalApiKey.isBlank()
                || !internalApiKey.equals(apiKey)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid internal API key"
            );
        }

        return userService.updateUserByAdmin(userId, request);
    }
}
