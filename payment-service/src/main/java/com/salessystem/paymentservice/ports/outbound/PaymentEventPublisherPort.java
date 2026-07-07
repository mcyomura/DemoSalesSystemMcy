package com.salessystem.paymentservice.ports.outbound;


import com.salessystem.paymentservice.domain.model.PaymentValidatedEventDTO;

public interface PaymentEventPublisherPort {
    // Publishes the payment result to the message broker
    void publish(PaymentValidatedEventDTO event, String orderUuid);
}