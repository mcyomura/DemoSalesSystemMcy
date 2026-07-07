package com.salessystem.paymentservice.adapters.config;


import com.salessystem.paymentservice.domain.usecase.ProcessPaymentUseCase;
import com.salessystem.paymentservice.ports.inbound.ProcessPaymentPort;
import com.salessystem.paymentservice.ports.outbound.PaymentEventPublisherPort;
import com.salessystem.paymentservice.ports.outbound.PaymentRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    /**
     * Instantiates the core Domain Use Case as a Spring Bean.
     * This decouples the domain layer from Spring framework annotations
     * while still allowing dependency injection across the application.
     */
    @Bean
    public ProcessPaymentPort processPaymentPort(PaymentRepositoryPort paymentRepositoryPort, PaymentEventPublisherPort paymentEventPublisherPort) {
        // We manually construct the pure Java usecase and pass its outbound port dependency
        return new ProcessPaymentUseCase(paymentRepositoryPort, paymentEventPublisherPort);
    }
}