package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HexFormat;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.client.UserServiceClient;
import com.example.demo.dto.UserAuthDetails;
import com.example.demo.entity.JWTToken;
import com.example.demo.repository.JWTTokenRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class AuthService {

    private static final long TOKEN_VALIDITY = 60 * 60 * 1000;

    private final UserServiceClient userServiceClient;
    private final JWTTokenRepository jwtTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey signingKey;
    private final String internalApiKey;

    public AuthService(
            UserServiceClient userServiceClient,
            JWTTokenRepository jwtTokenRepository,
            PasswordEncoder passwordEncoder,
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${internal.api-key}") String internalApiKey) {

        this.userServiceClient = userServiceClient;
        this.jwtTokenRepository = jwtTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.internalApiKey = internalApiKey;

        byte[] keyBytes =
                jwtSecret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 64) {
            throw new IllegalArgumentException(
                    "JWT secret must contain at least 64 bytes for HS512"
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // Authenticate user through User Service
    public UserAuthDetails authenticate(
            String username,
            String password) {

        try {
            UserAuthDetails user =
                    userServiceClient.getUserByUsername(
                            username,
                            internalApiKey
                    );

            if (!passwordEncoder.matches(
                    password,
                    user.passwordHash())) {

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"
                );
            }

            return user;

        } catch (ResponseStatusException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }
    }

    // Generate JWT and save its hash
    @Transactional
    public String generateToken(UserAuthDetails user) {

        LocalDateTime expiresAt =
                LocalDateTime.now().plusHours(1);

        Date issuedAt = new Date();
        Date expiration = new Date(
                issuedAt.getTime() + TOKEN_VALIDITY
        );

        String token = Jwts.builder()
                .setSubject(user.username())
                .claim("userId", user.userId())
                .claim("role", user.role().name())
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();

        // Revoke the previous active token for this user
        jwtTokenRepository.deleteByUserId(user.userId());

        JWTToken jwtToken = new JWTToken(
                user.userId(),
                hashToken(token),
                expiresAt
        );

        jwtTokenRepository.save(jwtToken);

        return token;
    }

    // Validate JWT signature, expiry and database session
    @Transactional(readOnly = true)
    public Claims validateToken(String token) {

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenHash = hashToken(token);

            JWTToken storedToken =
                    jwtTokenRepository.findByTokenHash(tokenHash)
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.UNAUTHORIZED,
                                            "Token is not active"
                                    )
                            );

            if (!storedToken.getExpiresAt()
                    .isAfter(LocalDateTime.now())) {

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Token has expired"
                );
            }

            return claims;

        } catch (ResponseStatusException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired token"
            );
        }
    }

    // Logout and revoke the token
    @Transactional
    public void logout(String token) {

        if (token == null || token.isBlank()) {
            return;
        }

        jwtTokenRepository.deleteByTokenHash(
                hashToken(token)
        );
    }

    // Generate SHA-256 hash of JWT
    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }
}