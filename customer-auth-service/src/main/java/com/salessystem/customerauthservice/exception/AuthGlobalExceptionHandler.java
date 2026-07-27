package com.salessystem.customerauthservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Clean centralized exception handler for the Customer Auth Service.
 */
@RestControllerAdvice
public class AuthGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalExceptionHandler.class);

    /**
     * Handles bad client requests, such as missing code parameters (HTTP 400).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid request payload received: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    /**
     * Handles OAuth authentication failures from GitHub (HTTP 401).
     */
    @ExceptionHandler(GithubAuthException.class)
    public ResponseEntity<Map<String, String>> handleGithubAuthException(GithubAuthException ex) {
        log.error("GitHub authentication error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    /**
     * Fallback handler for unexpected internal server errors (HTTP 500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: ", ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected internal error occurred.";
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", message);
    }

    /**
     * Helper method to build standard JSON response structure: { "error": "...", "message": "..." }
     */
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}