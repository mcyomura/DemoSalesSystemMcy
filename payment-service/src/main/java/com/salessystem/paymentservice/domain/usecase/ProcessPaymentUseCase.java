package com.salessystem.paymentservice.domain.usecase;

import com.salessystem.paymentservice.domain.model.PaymentTransaction;
import com.salessystem.paymentservice.domain.model.PaymentValidatedEventDTO;
import com.salessystem.paymentservice.domain.model.ServiceConfirmationStatus;
import com.salessystem.paymentservice.ports.inbound.ProcessPaymentPort;
import com.salessystem.paymentservice.ports.outbound.PaymentEventPublisherPort;
import com.salessystem.paymentservice.ports.outbound.PaymentRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Optional;

public class ProcessPaymentUseCase implements ProcessPaymentPort {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentUseCase.class);
    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentEventPublisherPort paymentEventPublisherPort;

    public ProcessPaymentUseCase(PaymentRepositoryPort paymentRepositoryPort, PaymentEventPublisherPort paymentEventPublisherPort) {
        this.paymentRepositoryPort = paymentRepositoryPort;
        this.paymentEventPublisherPort = paymentEventPublisherPort;
    }

    @Override
    public PaymentTransaction processPayment(String key, Integer orderId, Integer customerId, BigDecimal totalAmount, String paymentToken) {
        // if payment was already processed
        Optional<PaymentTransaction> paymentTransactionOpt = paymentRepositoryPort.findByOrderId(orderId);
        if (paymentTransactionOpt.isPresent()) {
            log.error("Payment duplicated!! Order ID: {}", orderId);
            return null;
        }

        // MOCK: in the demonstration we are not interfacing with external payment, therefore to simulate a declined
        // payment it's going to be a payment token ending with 99
        Integer paymentStatus = (paymentToken.endsWith("99"))
                ? ServiceConfirmationStatus.FAILED
                : ServiceConfirmationStatus.SUCCESS;

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId(orderId);
        transaction.setCustomerId(customerId);
        transaction.setTotalAmount(totalAmount);
        transaction.setPaymentToken(paymentToken);
        transaction.setStatus(paymentStatus);

        // Saves via Output Port
        PaymentTransaction savedTransaction = paymentRepositoryPort.save(transaction);

        String reason;
        if (transaction.getStatus() == ServiceConfirmationStatus.SUCCESS) {
            reason = "Payment processed and approved!";
            log.info("Payment approved! Order ID = " + orderId);
        } else {
            reason = "Payment declined..";
            log.info("Payment declined (in mock = payment token ending with 99)! Order ID = " + orderId);
        }

        // sends payment process event to order-service
        PaymentValidatedEventDTO event = new PaymentValidatedEventDTO(
                savedTransaction.getOrderId(), transaction.getStatus(), reason );

        paymentEventPublisherPort.publish(event, key);

        return savedTransaction;
    }

    @Override
    public void processRefund(String key, Integer orderId) {
        // Finds the existing transaction using Output Port
        Optional<PaymentTransaction> paymentTransactionOpt = paymentRepositoryPort.findByOrderId(orderId);
        if (paymentTransactionOpt.isPresent()) {
            PaymentTransaction transaction = paymentTransactionOpt.get();

            if (transaction.getStatus() == ServiceConfirmationStatus.SUCCESS) {

                transaction.setStatus(ServiceConfirmationStatus.PAYMENT_REFUNDED);
                // MOCK: in the demonstration we are not interfacing with external payment, this is the point to
                // call external payment and command a refund

                paymentRepositoryPort.save(transaction);
                log.info("Payment refunded! Order ID = " + orderId);

                // sends payment refund event to order-service
                PaymentValidatedEventDTO event = new PaymentValidatedEventDTO(
                        orderId, transaction.getStatus(), "Payment refunded!" );

                paymentEventPublisherPort.publish(event, key);

            } else if (transaction.getStatus() == ServiceConfirmationStatus.PAYMENT_REFUNDED) {
                log.error("Payment already refunded! Order ID = " + orderId);
            } else {
                log.error("Payment at invalid state for refund! Order ID = " + orderId);
            }

        } else {
            // if payment is not found then throws an error to be investigated. Order is kept pending
            log.error("Payment not found while trying to process a refund! Order ID = " + orderId);
        }



    }
}