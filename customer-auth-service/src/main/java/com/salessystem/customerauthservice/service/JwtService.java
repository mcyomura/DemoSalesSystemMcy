package com.salessystem.customerauthservice.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.salessystem.customerauthservice.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Service responsible for generating asymmetric JWT tokens and managing RSA KeyPair rotation.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final long expirationMs;

    // Current active RSA keys
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private Instant keyCreatedAt;

    /**
     * Constructor initializing keys on startup and loading expiration properties.
     */
    public JwtService(@Value("${jwt.expiration-ms}") long expirationMs) {
        this.expirationMs = expirationMs;
        // Generate initial key pair on startup
        this.rotateKeys();
    }

    /**
     * Generates a signed JWT token using the active RSA Private Key.
     *
     * @param user The authenticated user domain object
     * @return Signed JWT string
     */
    public String generateToken(User user) {
        try {
            Instant now = Instant.now();
            Instant expirationTime = now.plusMillis(expirationMs);

            // 1. Build claims set with user details
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .claim("email", user.getEmail())
                    .claim("githubId", user.getGithubId())
                    .claim("fullName", user.getFullName())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expirationTime))
                    .build();

            // 2. Prepare RSA Signer using the current Private Key
            JWSSigner signer = new RSASSASigner(this.privateKey);

            // 3. Create signed JWT object with RS256 algorithm
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.RS256),
                    claimsSet
            );

            // 4. Apply signature
            signedJWT.sign(signer);

            log.info("JWT token successfully generated for userId: {}", user.getId());

            // 5. Serialize to compact String format
            return signedJWT.serialize();

        } catch (Exception e) {
            log.error("Failed to sign asymmetric JWT token for userId: {}", user.getId(), e);
            throw new IllegalStateException("Error occurred while generating asymmetric JWT token", e);
        }
    }

    /**
     * Rotates the RSA KeyPair automatically every 24 hours (86,400,000 milliseconds).
     */
    @Scheduled(fixedRate = 86400000)
    public synchronized void rotateKeys() {
        KeyPair keyPair = generateRsaKeyPair();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.keyCreatedAt = Instant.now();
        log.info("RSA KeyPair successfully generated/rotated at {}", this.keyCreatedAt);
    }

    /**
     * Returns the active Public Key encoded in Base64 string format.
     *
     * @return Base64 encoded public key
     */
    public String getPublicKeyAsString() {
        return Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
    }

    /**
     * Returns the active RSA Public Key for signature validation.
     */
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Returns the creation timestamp of the current active RSA key pair.
     */
    public Instant getKeyCreatedAt() {
        return keyCreatedAt;
    }

    /**
     * Helper method to generate an RSA 2048-bit KeyPair in memory.
     */
    private KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.error("Crypto environment error: RSA algorithm is missing", e);
            throw new IllegalStateException("RSA algorithm not available in current environment", e);
        }
    }
}