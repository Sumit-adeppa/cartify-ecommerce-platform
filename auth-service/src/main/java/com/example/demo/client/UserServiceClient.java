package com.example.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.example.demo.dto.UserAuthDetails;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/internal/users/by-username/{username}")
    UserAuthDetails getUserByUsername(
            @PathVariable("username") String username,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    );
}