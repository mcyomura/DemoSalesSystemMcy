package com.salessystem.orderservice.application.exception;

public class IllegalOrderStateException extends RuntimeException {
    public IllegalOrderStateException(String message) { super(message); }
}

