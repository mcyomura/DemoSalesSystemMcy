package com.salessystem.bff.controller;


import com.salessystem.bff.client.CustomerAuthFeignClient;
import com.salessystem.bff.dto.auth.AuthRequest;
import com.salessystem.bff.dto.auth.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing authentication endpoints to front-end / API clients.
 */
@RestController
@RequestMapping("${app.api.path.bff}")
public class AuthBffController {

    private final CustomerAuthFeignClient authFeignClient;
    private static final Logger log = LoggerFactory.getLogger(AuthBffController.class);

    public AuthBffController(CustomerAuthFeignClient authFeignClient) {
        this.authFeignClient = authFeignClient;
    }

    /**
     * Receives OAuth callback from GitHub redirect with authorization code.
     *
     * @param code Authorization code from GitHub
     * @return ResponseEntity with JWT token
     */
    @GetMapping("/auth2/callback")
    public ResponseEntity<AuthResponse> handleGithubCallback(@RequestParam("code") String code) {
        AuthRequest request = new AuthRequest(code);
        ResponseEntity<AuthResponse> authResponseResponseEntity = authFeignClient.loginWithGithub(request);

        return authResponseResponseEntity;
    }
}