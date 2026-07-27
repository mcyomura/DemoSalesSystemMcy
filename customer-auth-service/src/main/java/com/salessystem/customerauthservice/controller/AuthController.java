package com.salessystem.customerauthservice.controller;


import com.salessystem.customerauthservice.dto.AuthRequest;
import com.salessystem.customerauthservice.dto.AuthResponse;
import com.salessystem.customerauthservice.service.AuthService;
import com.salessystem.customerauthservice.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller exposing authentication endpoints for the BFF.
 */
@RestController
@RequestMapping("${app.api.path.auth}")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    /**
     * Receives GitHub OAuth code and returns application JWT token.
     *
     * @param request AuthRequest containing authorization code
     * @return ResponseEntity containing AuthResponse
     */
    @PostMapping("/github")
    public ResponseEntity<AuthResponse> loginWithGithub(@RequestBody AuthRequest request) {
        AuthResponse response = authService.processGithubLogin(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint exposing active RSA Public Key for JWT validation by other services.
     *
     * @return ResponseEntity with Base64 encoded public key
     */
    @GetMapping("/public-key")
    public ResponseEntity<String> getPublicKey() {

        return ResponseEntity.ok(jwtService.getPublicKeyAsString());
    }
}