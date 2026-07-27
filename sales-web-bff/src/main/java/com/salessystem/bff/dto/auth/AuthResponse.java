package com.salessystem.bff.dto.auth;

/**
 * DTO representing the authentication response returned to the BFF.
 */
public class AuthResponse {

    private String token;
    private String tokenType;
    private String email;
    private String fullName;

    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType, String email, String fullName) {
        this.token = token;
        this.tokenType = tokenType;
        this.email = email;
        this.fullName = fullName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}