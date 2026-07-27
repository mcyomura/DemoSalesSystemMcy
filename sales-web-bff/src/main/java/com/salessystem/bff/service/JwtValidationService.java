package com.salessystem.bff.service;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.salessystem.bff.client.CustomerAuthFeignClient;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * Service responsible for validating asymmetric JWT tokens using customer-auth-service public key.
 */
@Service
public class JwtValidationService {

    private final CustomerAuthFeignClient authFeignClient;
    private RSAPublicKey cachedPublicKey;

    public JwtValidationService(CustomerAuthFeignClient authFeignClient) {
        this.authFeignClient = authFeignClient;
    }

    /**
     * Validates JWT signature and expiration time. Retries fetching public key once if signature verification fails.
     *
     * @param token Compact JWT string
     * @return JWTClaimsSet if token is valid
     * @throws Exception if token is invalid or expired
     */
    public JWTClaimsSet validateToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Ensure key is loaded
        if (cachedPublicKey == null) {
            refreshPublicKey();
        }

        // 1. Verify signature with cached key
        boolean isSignatureValid = verifySignature(signedJWT, this.cachedPublicKey);

        // Fallback: If signature failed, key might have rotated. Force refresh and retry once.
        if (!isSignatureValid) {
            refreshPublicKey();
            isSignatureValid = verifySignature(signedJWT, this.cachedPublicKey);
            if (!isSignatureValid) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }
        }

        // 2. Verify expiration time
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        if (claimsSet.getExpirationTime().before(new Date())) {
            throw new IllegalArgumentException("JWT token has expired");
        }

        return claimsSet;
    }

    private boolean verifySignature(SignedJWT signedJWT, RSAPublicKey key) {
        try {
            JWSVerifier verifier = new RSASSAVerifier(key);
            return signedJWT.verify(verifier);
        } catch (Exception e) {
            return false;
        }
    }

    private synchronized void refreshPublicKey() {
        try {
            String publicKeyStr = authFeignClient.getPublicKey().getBody();
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.cachedPublicKey = (RSAPublicKey) keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to retrieve public key from customer-auth-service", e);
        }
    }
}