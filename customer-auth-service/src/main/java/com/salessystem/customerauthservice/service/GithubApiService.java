package com.salessystem.customerauthservice.service;

import com.salessystem.customerauthservice.dto.GithubTokenResponse;
import com.salessystem.customerauthservice.dto.GithubUserResponse;
import com.salessystem.customerauthservice.exception.GithubAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for HTTP communication with GitHub OAuth and User APIs using RestClient.
 */
@Service
public class GithubApiService {

    private static final Logger log = LoggerFactory.getLogger(GithubApiService.class);

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GithubApiService(
            @Value("${github.client-id}") String clientId,
            @Value("${github.client-secret}") String clientSecret,
            @Value("${github.redirect-uri}") String redirectUri) {
        this.restClient = RestClient.create();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /**
     * Exchanges authorization code for GitHub Access Token.
     *
     * @param code Authorization code received from OAuth callback
     * @return GithubTokenResponse containing the access token
     */
    public GithubTokenResponse getAccessToken(String code) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("client_id", clientId);
            requestBody.put("client_secret", clientSecret);
            requestBody.put("code", code);
            requestBody.put("redirect_uri", redirectUri);

            return restClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(GithubTokenResponse.class);
        } catch (RestClientResponseException e) {
            log.error("GitHub API error during token exchange. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GithubAuthException("GitHub returned error during token exchange: " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            log.error("Network or timeout error connecting to GitHub OAuth API: ", e);
            throw new GithubAuthException("Could not connect to GitHub authentication service", e);
        } catch (Exception e) {
            log.error("Unexpected error exchanging GitHub authorization code: ", e);
            throw new GithubAuthException("Failed to exchange GitHub authorization code", e);
        }
    }

    /**
     * Fetches user profile from GitHub API and handles private emails fallback.
     *
     * @param accessToken GitHub Access Token
     * @return GithubUserResponse populated with user data and valid email
     */
    public GithubUserResponse getUserProfile(String accessToken) {
        try {
            return restClient.get()
                    .uri("https://api.github.com/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GithubUserResponse.class);
        } catch (RestClientResponseException e) {
            log.error("GitHub API error fetching user profile. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GithubAuthException("Failed to fetch GitHub user profile: " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            log.error("Network or timeout error connecting to GitHub User API: ", e);
            throw new GithubAuthException("Could not connect to GitHub user profile service", e);
        } catch (Exception e) {
            log.error("Unexpected error fetching GitHub user profile: ", e);
            throw new GithubAuthException("Failed to fetch user profile from GitHub", e);
        }
    }
}