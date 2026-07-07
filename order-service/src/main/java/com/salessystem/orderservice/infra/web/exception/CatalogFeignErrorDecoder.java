package com.salessystem.orderservice.infra.web.exception;


import com.salessystem.orderservice.application.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;

// This class (ErrorDecoder) is part of the Spring Cloud / Feign ecosystem
// it will handle errors from the feign call and transforms in a use case exception that will be handled by the GlobalExceptionHandler
// configured at CatalogFeignConfig
public class CatalogFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        // 1. Verify
        if (response.status() == 404) {
            return new ResourceNotFoundException("Product not found in catalog");
        }

        // 2. Other errors, default treatment by feign
        return defaultErrorDecoder.decode(methodKey, response);
    }
}