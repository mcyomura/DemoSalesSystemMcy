package com.salessystem.customerauthservice.service;

import com.salessystem.customerauthservice.dto.AuthRequest;
import com.salessystem.customerauthservice.dto.AuthResponse;
import com.salessystem.customerauthservice.dto.GithubTokenResponse;
import com.salessystem.customerauthservice.dto.GithubUserResponse;
import com.salessystem.customerauthservice.exception.GithubAuthException;
import com.salessystem.customerauthservice.model.User;
import com.salessystem.customerauthservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService verifying orchestration between GitHub API, UserRepository, and JwtService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private GithubApiService githubApiService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should successfully process GitHub login, save/update user in DB, and return AuthResponse")
    void processGithubLogin_ShouldAuthenticateAndReturnAuthResponse() {
        // Arrange
        String code = "valid_github_code";
        AuthRequest authRequest = new AuthRequest(code);

        GithubTokenResponse tokenResponse = new GithubTokenResponse();
        tokenResponse.setAccessToken("gho_mock_access_token");

        GithubUserResponse userResponse = new GithubUserResponse();
        userResponse.setId(12345L);
        userResponse.setLogin("githubuser");
        userResponse.setName("Test User");
        userResponse.setEmail("user@test.com");
        userResponse.setAvatarUrl("https://github.com/avatar.png");

        User savedUser = new User();
        savedUser.setId(1);
        savedUser.setGithubId("12345");
        savedUser.setEmail("user@test.com");
        savedUser.setFullName("Test User");

        when(githubApiService.getAccessToken(code)).thenReturn(tokenResponse);
        when(githubApiService.getUserProfile("gho_mock_access_token")).thenReturn(userResponse);
        when(userRepository.findByGithubId("12345")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mocked_signed_jwt_token");

        // Act
        AuthResponse response = authService.processGithubLogin(authRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mocked_signed_jwt_token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("user@test.com", response.getEmail());
        assertEquals("Test User", response.getFullName());

        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when OAuth code is null or empty")
    void processGithubLogin_ShouldThrowIllegalArgumentExceptionWhenCodeIsMissing() {
        // Arrange
        AuthRequest emptyRequest = new AuthRequest("   ");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.processGithubLogin(emptyRequest));
        verify(githubApiService, never()).getAccessToken(anyString());
    }

    @Test
    @DisplayName("Should propagate GithubAuthException when GitHub token exchange fails")
    void processGithubLogin_ShouldPropagateExceptionWhenGithubFails() {
        // Arrange
        String invalidCode = "invalid_code";
        AuthRequest authRequest = new AuthRequest(invalidCode);

        when(githubApiService.getAccessToken(invalidCode))
                .thenThrow(new GithubAuthException("Failed to exchange GitHub authorization code"));

        // Act & Assert
        assertThrows(GithubAuthException.class, () -> authService.processGithubLogin(authRequest));
        verify(userRepository, never()).save(any(User.class));
    }
}