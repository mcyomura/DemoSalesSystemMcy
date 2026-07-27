package com.salessystem.customerauthservice.service;

import com.salessystem.customerauthservice.dto.AuthRequest;
import com.salessystem.customerauthservice.dto.AuthResponse;
import com.salessystem.customerauthservice.dto.GithubTokenResponse;
import com.salessystem.customerauthservice.dto.GithubUserResponse;
import com.salessystem.customerauthservice.model.User;
import com.salessystem.customerauthservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service orchestrating authentication logic, database persistence, and token issuance.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final GithubApiService githubApiService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(GithubApiService githubApiService, UserRepository userRepository, JwtService jwtService) {
        this.githubApiService = githubApiService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * Authenticates a customer using GitHub code, updates MariaDB, and generates JWT.
     *
     * @param request AuthRequest containing authorization code
     * @return AuthResponse containing signed JWT and user information
     */
    public AuthResponse processGithubLogin(AuthRequest request) {
        if (request == null || request.getCode() == null || request.getCode().trim().isEmpty()) {
            log.warn("Authentication request rejected: OAuth code is missing or empty.");
            throw new IllegalArgumentException("Authorization code must not be null or empty.");
        }

        log.info("Starting GitHub authentication process for code...");

        // 1. Exchange code for access token
        GithubTokenResponse tokenResponse = githubApiService.getAccessToken(request.getCode());

        // 2. Fetch user profile from GitHub
        GithubUserResponse githubUser = githubApiService.getUserProfile(tokenResponse.getAccessToken());

        // 3. Find existing user or create a new one
        String githubId = githubUser.getId().toString();
        User user = userRepository.findByGithubId(githubId)
                .orElseGet(() -> {
                    log.info("Creating new user account for GitHub ID: {}", githubId);
                    return new User();
                });

        user.setGithubId(githubId);
        user.setEmail(githubUser.getEmail());
        user.setFullName(githubUser.getName() != null ? githubUser.getName() : githubUser.getLogin());
        user.setAvatarUrl(githubUser.getAvatarUrl());
        user.setLastLoginAt(LocalDateTime.now());

        // Save or update user in MariaDB
        User savedUser = userRepository.save(user);
        log.info("User details successfully persisted in MariaDB with ID: {}", savedUser.getId());

        // 4. Generate asymmetric JWT token
        String jwtToken = jwtService.generateToken(savedUser);

        return new AuthResponse(
                jwtToken,
                "Bearer",
                savedUser.getEmail(),
                savedUser.getFullName()
        );
    }
}