package com.salessystem.customerauthservice.service;

import com.nimbusds.jwt.SignedJWT;
import com.salessystem.customerauthservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying JWT token creation, claims mapping, and RSA key pair rotation logic in JwtService.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private final long expirationMs = 3600000L; // 1 hour expiration for tests

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(expirationMs);
    }

    @Test
    @DisplayName("Should generate a valid signed JWT token containing user details in claims")
    void shouldGenerateValidJwtTokenForUser() throws Exception {
        // Arrange
        User user = new User();
        user.setId(100);
        user.setEmail("dev@test.com");
        user.setGithubId("998877");
        user.setFullName("Java Developer");

        // Act
        String token = jwtService.generateToken(user);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);

        // Parse and inspect JWT claims
        SignedJWT signedJWT = SignedJWT.parse(token);
        assertEquals("100", signedJWT.getJWTClaimsSet().getSubject());
        assertEquals("dev@test.com", signedJWT.getJWTClaimsSet().getStringClaim("email"));
        assertEquals("998877", signedJWT.getJWTClaimsSet().getStringClaim("githubId"));
        assertEquals("Java Developer", signedJWT.getJWTClaimsSet().getStringClaim("fullName"));
    }

    @Test
    @DisplayName("Should return RSA public key in valid Base64 string format")
    void shouldReturnBase64EncodedPublicKey() {
        // Act
        String publicKeyBase64 = jwtService.getPublicKeyAsString();

        // Assert
        assertNotNull(publicKeyBase64);
        assertTrue(publicKeyBase64.length() > 100);
        assertNotNull(jwtService.getPublicKey());
    }

    @Test
    @DisplayName("Should successfully rotate RSA key pair and update key generation timestamp")
    void shouldRotateRsaKeysSuccessfully() {
        // Arrange
        String initialPublicKey = jwtService.getPublicKeyAsString();
        Instant initialTimestamp = jwtService.getKeyCreatedAt();

        // Act
        jwtService.rotateKeys();

        // Assert
        String rotatedPublicKey = jwtService.getPublicKeyAsString();
        Instant rotatedTimestamp = jwtService.getKeyCreatedAt();

        assertNotEquals(initialPublicKey, rotatedPublicKey);
        assertNotEquals(initialTimestamp, rotatedTimestamp);
    }
}