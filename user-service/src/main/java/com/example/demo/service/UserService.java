package com.example.demo.service;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.dto.AdminUserUpdateRequest;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;

import com.example.demo.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Register a new customer
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {

        if (userRepository.existsByUsername(
                request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        if (userRepository.existsByEmail(
                request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already exists"
            );
        }

        User user = new User();

        user.setUsername(request.username());
        user.setEmail(request.email());

        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        // Never accept ADMIN role from registration input.
        user.setRole(Role.CUSTOMER);

        User savedUser = userRepository.save(user);

        return toUserResponse(savedUser);
    }

    // Get user by ID
    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        return toUserResponse(user);
    }

    // Update username and email
    @Transactional
    public UserResponse updateProfile(
            Integer userId,
            UpdateProfileRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        userRepository.findByUsername(request.username())
                .filter(existing ->
                        !existing.getUserId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Username is already taken"
                    );
                });

        userRepository.findByEmail(request.email())
                .filter(existing ->
                        !existing.getUserId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Email is already registered"
                    );
                });

        user.setUsername(request.username());
        user.setEmail(request.email());

        User updatedUser = userRepository.save(user);

        return toUserResponse(updatedUser);
    }

    // Admin updates user details
    @Transactional
    public UserResponse updateUserByAdmin(
            Integer userId,
            AdminUserUpdateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        if (request.username() != null
                && !request.username().isBlank()) {

            String username = request.username().trim();

            userRepository.findByUsername(username)
                    .filter(existing ->
                            !existing.getUserId().equals(userId))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Username is already taken"
                        );
                    });

            user.setUsername(username);
        }

        if (request.email() != null
                && !request.email().isBlank()) {

            String email = request.email().trim();

            userRepository.findByEmail(email)
                    .filter(existing ->
                            !existing.getUserId().equals(userId))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Email is already registered"
                        );
                    });

            user.setEmail(email);
        }

        if (request.role() != null
                && !request.role().isBlank()) {

            try {
                Role role = Role.valueOf(
                        request.role().trim().toUpperCase()
                );

                user.setRole(role);

            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid role. Allowed roles: ADMIN, CUSTOMER"
                );
            }
        }

        User savedUser = userRepository.save(user);

        return toUserResponse(savedUser);
    }

    // Convert Entity to Response DTO
    private UserResponse toUserResponse(User user) {

        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
