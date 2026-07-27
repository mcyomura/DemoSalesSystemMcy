package com.salessystem.bff.config;

import com.nimbusds.jwt.JWTClaimsSet;
import com.salessystem.bff.service.JwtValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JwtAuthenticationInterceptor verifying request interception and JWT authorization logic.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationInterceptorTest {

    @Mock
    private JwtValidationService jwtValidationService;

    @InjectMocks
    private JwtAuthenticationInterceptor interceptor;

    @Test
    @DisplayName("Should return 401 Unauthorized when Authorization header is missing")
    void shouldReturnUnauthorizedWhenHeaderIsMissing() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertFalse(result);
        assertEquals(401, response.getStatus());
        assertEquals("Missing or invalid Authorization header", response.getContentAsString());
        verifyNoInteractions(jwtValidationService);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when Authorization header does not start with Bearer")
    void shouldReturnUnauthorizedWhenHeaderIsNotBearer() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic invalid_token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertFalse(result);
        assertEquals(401, response.getStatus());
        assertEquals("Missing or invalid Authorization header", response.getContentAsString());
        verifyNoInteractions(jwtValidationService);
    }

    @Test
    @DisplayName("Should allow request and set authenticatedUserId attribute when token is valid")
    void shouldAllowRequestWhenTokenIsValid() throws Exception {
        // Arrange
        String token = "valid_jwt_token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("user_12345")
                .build();

        when(jwtValidationService.validateToken(token)).thenReturn(claimsSet);

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertTrue(result);
        assertEquals("user_12345", request.getAttribute("authenticatedUserId"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when JwtValidationService throws exception")
    void shouldReturnUnauthorizedWhenTokenValidationFails() throws Exception {
        // Arrange
        String token = "invalid_jwt_token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtValidationService.validateToken(token))
                .thenThrow(new IllegalArgumentException("JWT token has expired"));

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertFalse(result);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Authentication failed: JWT token has expired"));
    }
}