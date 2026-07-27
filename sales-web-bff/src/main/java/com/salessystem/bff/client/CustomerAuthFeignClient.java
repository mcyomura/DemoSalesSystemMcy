package com.salessystem.bff.client;

import com.salessystem.bff.dto.auth.AuthRequest;
import com.salessystem.bff.dto.auth.AuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declarative Feign client for communicating with customer-auth-service.
 */
@FeignClient(name = "customer-auth-service", url = "${app.services.customer-auth.url}", path = "${app.services.customer-auth.path}")
public interface CustomerAuthFeignClient {

    /**
     * Forwards OAuth code to customer-auth-service to exchange for JWT.
     *
     * @param request AuthRequest containing authorization code
     * @return ResponseEntity containing AuthResponse with JWT
     */
    @PostMapping("/github")
    ResponseEntity<AuthResponse> loginWithGithub(@RequestBody AuthRequest request);

    /**
     * Gets the public key to validate JWT locally.
     */
    @GetMapping("/public-key")
    ResponseEntity<String> getPublicKey();
}