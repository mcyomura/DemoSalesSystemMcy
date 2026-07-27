package com.salessystem.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salessystem.bff.client.CustomerAuthFeignClient;
import com.salessystem.bff.dto.auth.AuthRequest;
import com.salessystem.bff.dto.auth.AuthResponse;
import com.salessystem.bff.exception.BffGlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AuthBffController verifying endpoints handling OAuth callback request.
 */
@WebMvcTest(AuthBffController.class)
@Import(BffGlobalExceptionHandler.class) // Explicitly imports global exception handler into test context
@AutoConfigureMockMvc(addFilters = false)
class AuthBffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CustomerAuthFeignClient authFeignClient;

    @Test
    @DisplayName("Should process GET callback request with code parameter successfully")
    void shouldHandleGithubCallbackSuccessfully() throws Exception {
        // Arrange
        String code = "valid_github_code";
        AuthResponse mockResponse = new AuthResponse("mock-jwt-token", "Bearer", "user@test.com", "Test User");

        when(authFeignClient.loginWithGithub(any(AuthRequest.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act & Assert
        mockMvc.perform(get("/api/v1/salesbff/auth2/callback")
                        .param("code", code)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));

        verify(authFeignClient).loginWithGithub(any(AuthRequest.class));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when GitHub authentication fails in customer-auth-service")
    void shouldReturnServerErrorWhenGithubAuthFails() throws Exception {
        // Arrange
        when(authFeignClient.loginWithGithub(any(AuthRequest.class)))
                .thenThrow(new RuntimeException("GitHub authorization denied or code invalid"));

        // Act & Assert: Handler now catches the exception and converts it into HTTP 500
        mockMvc.perform(get("/api/v1/salesbff/auth2/callback")
                        .param("code", "invalid_or_denied_code")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(authFeignClient).loginWithGithub(any(AuthRequest.class));
    }
}