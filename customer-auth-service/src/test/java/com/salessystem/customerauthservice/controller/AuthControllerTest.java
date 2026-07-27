package com.salessystem.customerauthservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salessystem.customerauthservice.dto.AuthRequest;
import com.salessystem.customerauthservice.dto.AuthResponse;
import com.salessystem.customerauthservice.exception.AuthGlobalExceptionHandler;
import com.salessystem.customerauthservice.exception.GithubAuthException;
import com.salessystem.customerauthservice.service.AuthService;
import com.salessystem.customerauthservice.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AuthController verifying REST endpoints and exception handling integration.
 */
@WebMvcTest(AuthController.class)
@Import(AuthGlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "app.api.path.auth=/api/v1/auth")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("Should return 200 OK with AuthResponse when GitHub login code is valid")
    void loginWithGithub_ShouldReturnOk() throws Exception {
        // Arrange
        AuthRequest authRequest = new AuthRequest("valid_github_code");
        AuthResponse mockResponse = new AuthResponse("mocked_jwt_token", "Bearer", "user@test.com", "Test User");

        when(authService.processGithubLogin(any(AuthRequest.class))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked_jwt_token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));

        verify(authService).processGithubLogin(any(AuthRequest.class));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when GitHub authentication fails in service")
    void loginWithGithub_ShouldReturnUnauthorizedOnFailure() throws Exception {
        // Arrange
        AuthRequest authRequest = new AuthRequest("invalid_code");

        when(authService.processGithubLogin(any(AuthRequest.class)))
                .thenThrow(new GithubAuthException("GitHub authorization denied or code invalid"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("GitHub authorization denied or code invalid"));

        verify(authService).processGithubLogin(any(AuthRequest.class));
    }

    @Test
    @DisplayName("Should return 200 OK with active Base64 RSA public key string")
    void getPublicKey_ShouldReturnBase64PublicKeyString() throws Exception {
        // Arrange
        String mockPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu...";

        when(jwtService.getPublicKeyAsString()).thenReturn(mockPublicKey);

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/public-key"))
                .andExpect(status().isOk())
                .andExpect(content().string(mockPublicKey));

        verify(jwtService).getPublicKeyAsString();
    }
}