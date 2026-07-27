package com.salessystem.bff.exception;

import com.salessystem.bff.controller.CartBffController;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class BffGlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(BffGlobalExceptionHandler.class);

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<String> handleFeignException(FeignException e) {
        // Here we extract the exact HTTP Status code that came from the downstream microservice
        int status = e.status();
        String responseBody = e.contentUTF8();

        // If it's a known error (like 400, 404, 409), we relay it transparently to the frontend
        if (status >= 400 && status < 500) {
            return ResponseEntity.status(status).body(responseBody);
        }

        log.info("Feign call returned error from destiny service - status {}, responsebody: {}", status, responseBody);
        // If it's a 500 or network failure, we hide the raw stacktrace and return a friendly error
        return ResponseEntity.status(500).body("An internal error occurred in our downstream services.");
    }

    /**
     * Handles uncaught generic exceptions and runtime failures across the BFF application.
     *
     * @param e Uncaught Exception instance
     * @return ResponseEntity with HTTP 500 status and friendly message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        log.error("Unhandled exception captured by global handler: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred: " + e.getMessage());
    }
}