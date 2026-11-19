package com.example.demo.controller;

import com.example.demo.dto.AdminUserUpdateRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.service.AdminService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PutMapping("/modify")
    public ResponseEntity<UserResponse> modifyUser(
            @Valid @RequestBody AdminUserUpdateRequest request) {

        return ResponseEntity.ok(adminService.modifyUser(request));
    }

    @GetMapping("/getbyid")
    public ResponseEntity<UserResponse> getUserById(
            @RequestParam Integer userId) {

        return ResponseEntity.ok(adminService.getUserById(userId));
    }
}