package com.salessystem.paymentservice.ports.outbound;

import com.salessystem.paymentservice.domain.model.PaymentTransaction;
import java.util.Optional;

public interface PaymentRepositoryPort {
    // Saves a payment transaction into the database
    PaymentTransaction save(PaymentTransaction paymentTransaction);

    // Finds a payment transaction by the order identifier
    Optional<PaymentTransaction> findByOrderId(Integer orderId);
}