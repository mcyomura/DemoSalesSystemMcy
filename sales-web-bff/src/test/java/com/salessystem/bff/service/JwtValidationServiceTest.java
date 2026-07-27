package com.salessystem.bff.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.salessystem.bff.client.CustomerAuthFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JwtValidationService verifying RSA JWT signature and expiration logic.
 */
@ExtendWith(MockitoExtension.class)
class JwtValidationServiceTest {

    @Mock
    private CustomerAuthFeignClient authFeignClient;

    @InjectMocks
    private JwtValidationService jwtValidationService;

    private KeyPair cachedKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        // Generate temporary RSA KeyPair for test token signing
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        this.cachedKeyPair = generator.generateKeyPair();
    }

    @Test
    @DisplayName("Should validate valid JWT token successfully")
    void shouldValidateValidTokenSuccessfully() throws Exception {
        // Arrange
        String publicKeyBase64 = Base64.getEncoder().encodeToString(cachedKeyPair.getPublic().getEncoded());
        when(authFeignClient.getPublicKey()).thenReturn(ResponseEntity.ok(publicKeyBase64));

        String validToken = generateTestJwt(cachedKeyPair, 100000);

        // Act
        JWTClaimsSet claims = jwtValidationService.validateToken(validToken);

        // Assert
        assertNotNull(claims);
        assertEquals("12345", claims.getSubject());
        assertEquals("test@example.com", claims.getClaim("email"));
        verify(authFeignClient, times(1)).getPublicKey();
    }

    @Test
    @DisplayName("Should throw exception when validating expired JWT token")
    void shouldThrowExceptionWhenTokenIsExpired() throws Exception {
        // Arrange
        String publicKeyBase64 = Base64.getEncoder().encodeToString(cachedKeyPair.getPublic().getEncoded());
        when(authFeignClient.getPublicKey()).thenReturn(ResponseEntity.ok(publicKeyBase64));

        String expiredToken = generateTestJwt(cachedKeyPair, -100000);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtValidationService.validateToken(expiredToken)
        );

        assertEquals("JWT token has expired", exception.getMessage());
    }

    @Test
    @DisplayName("Should refresh public key and validate successfully when initial signature fails due to key rotation")
    void shouldRefreshPublicKeyAndValidateSuccessfullyOnKeyRotation() throws Exception {
        // Arrange: Generate a second key pair simulating rotated keys in customer-auth-service
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair refreshedKeyPair = generator.generateKeyPair();

        String oldPublicKeyBase64 = Base64.getEncoder().encodeToString(cachedKeyPair.getPublic().getEncoded());
        String refreshedPublicKeyBase64 = Base64.getEncoder().encodeToString(refreshedKeyPair.getPublic().getEncoded());

        // First call returns old public key, second call returns the new rotated public key
        when(authFeignClient.getPublicKey())
                .thenReturn(ResponseEntity.ok(oldPublicKeyBase64))
                .thenReturn(ResponseEntity.ok(refreshedPublicKeyBase64));

        // Generate a valid JWT signed with the NEW private key
        String tokenSignedWithNewKey = generateTestJwt(refreshedKeyPair, 100000);

        // Act: First validation will fail signature with cached old key, trigger refreshPublicKey(), and succeed
        JWTClaimsSet claims = jwtValidationService.validateToken(tokenSignedWithNewKey);

        // Assert
        assertNotNull(claims);
        assertEquals("12345", claims.getSubject());
        assertEquals("test@example.com", claims.getClaim("email"));

        // Verify that getPublicKey was called twice (once for initial cache load, once during fallback)
        verify(authFeignClient, times(2)).getPublicKey();
    }

    /**
     * Helper method to construct signed test JWT using a specific RSA KeyPair.
     */
    private String generateTestJwt(KeyPair customKeyPair, long expirationOffsetMs) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("12345")
                .claim("email", "test@example.com")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(expirationOffsetMs)))
                .build();

        JWSSigner signer = new RSASSASigner((RSAPrivateKey) customKeyPair.getPrivate());
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }
}