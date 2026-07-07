package com.salessystem.paymentservice.ports.inbound;

import com.salessystem.paymentservice.domain.model.PaymentTransaction;
import java.math.BigDecimal;

public interface ProcessPaymentPort {
    // Executes the payment business logic for a new order
    PaymentTransaction processPayment(String key, Integer orderId, Integer customerId, BigDecimal totalAmount, String paymentToken);

    // Executes the refund business logic for a canceled order
    void processRefund(String key, Integer orderId);
}