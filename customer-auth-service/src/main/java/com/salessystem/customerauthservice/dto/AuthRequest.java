package com.salessystem.customerauthservice.dto;

/**
 * DTO representing the authentication request sent by the BFF with the GitHub code.
 */
public class AuthRequest {

    private String code;

    public AuthRequest() {
    }

    public AuthRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}