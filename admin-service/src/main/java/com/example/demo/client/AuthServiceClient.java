package com.example.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @DeleteMapping("/internal/auth/users/{userId}/tokens")
    void revokeUserTokens(
            @PathVariable("userId") Integer userId,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    );
}