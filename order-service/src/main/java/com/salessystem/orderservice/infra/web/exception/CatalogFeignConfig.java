package com.salessystem.orderservice.infra.web.exception;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class CatalogFeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CatalogFeignErrorDecoder();
    }
}