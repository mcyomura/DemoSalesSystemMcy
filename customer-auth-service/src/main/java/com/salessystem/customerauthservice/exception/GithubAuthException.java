package com.salessystem.customerauthservice.exception;

/**
 * Custom runtime exception thrown when GitHub OAuth or API calls fail.
 */
public class GithubAuthException extends RuntimeException {

    public GithubAuthException(String message) {
        super(message);
    }

    public GithubAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}