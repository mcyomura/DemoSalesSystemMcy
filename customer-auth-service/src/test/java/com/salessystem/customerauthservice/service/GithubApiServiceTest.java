package com.salessystem.customerauthservice.service;

import com.salessystem.customerauthservice.dto.GithubTokenResponse;
import com.salessystem.customerauthservice.dto.GithubUserResponse;
import com.salessystem.customerauthservice.exception.GithubAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Unit tests verifying GitHub API communication and HTTP error handling in GithubApiService.
 */
class GithubApiServiceTest {

    private GithubApiService githubApiService;
    private MockRestServiceServer mockServer;

    private final String clientId = "test-client-id";
    private final String clientSecret = "test-client-secret";
    private final String redirectUri = "http://localhost:8080/callback";

    @BeforeEach
    void setUp() {
        // Construct RestClient Builder and bind MockRestServiceServer to simulate GitHub HTTP server
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        // Instantiate service manually with mock dependencies
        githubApiService = new GithubApiService(clientId, clientSecret, redirectUri) {
            // Override RestClient initialization to use our bound mock builder
            {
                try {
                    var field = GithubApiService.class.getDeclaredField("restClient");
                    field.setAccessible(true);
                    field.set(this, restClientBuilder.build());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    @DisplayName("Should exchange authorization code for access token successfully")
    void getAccessToken_ShouldReturnTokenResponse() {
        // Arrange
        String code = "valid_github_code";
        String mockJsonResponse = """
                {
                    "access_token": "gho_16C5G808000000000000000000000000",
                    "token_type": "bearer",
                    "scope": "read:user,user:email"
                }
                """;

        mockServer.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        // Act
        GithubTokenResponse response = githubApiService.getAccessToken(code);

        // Assert
        assertNotNull(response);
        assertEquals("gho_16C5G808000000000000000000000000", response.getAccessToken());
        assertEquals("bearer", response.getTokenType());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw GithubAuthException when token exchange fails with 400 Bad Request")
    void getAccessToken_ShouldThrowGithubAuthExceptionOnError() {
        // Arrange
        String invalidCode = "expired_code";

        mockServer.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest());

        // Act & Assert
        assertThrows(GithubAuthException.class, () -> githubApiService.getAccessToken(invalidCode));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should fetch GitHub user profile successfully using valid access token")
    void getUserProfile_ShouldReturnUserProfile() {
        // Arrange
        String accessToken = "gho_valid_token";
        String mockJsonResponse = """
                {
                    "id": 123456,
                    "login": "octocat",
                    "name": "Monomorphic Octocat",
                    "email": "octocat@github.com",
                    "avatar_url": "https://github.com/images/error/octocat_happy.gif"
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/user"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + accessToken))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        // Act
        GithubUserResponse response = githubApiService.getUserProfile(accessToken);

        // Assert
        assertNotNull(response);
        assertEquals(123456L, response.getId());
        assertEquals("octocat", response.getLogin());
        assertEquals("octocat@github.com", response.getEmail());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw GithubAuthException when fetching user profile fails with 401 Unauthorized")
    void getUserProfile_ShouldThrowGithubAuthExceptionOnError() {
        // Arrange
        String invalidToken = "gho_invalid_or_expired_token";

        mockServer.expect(requestTo("https://api.github.com/user"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + invalidToken))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        // Act & Assert
        assertThrows(GithubAuthException.class, () -> githubApiService.getUserProfile(invalidToken));
        mockServer.verify();
    }
}