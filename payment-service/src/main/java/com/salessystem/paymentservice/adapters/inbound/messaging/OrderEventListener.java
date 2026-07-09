package com.salessystem.paymentservice.adapters.inbound.messaging;

import com.salessystem.paymentservice.ports.inbound.ProcessPaymentPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    // Official slf4j logger implementation
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final ProcessPaymentPort processPaymentPort;

    public OrderEventListener(ProcessPaymentPort processPaymentPort) {
        this.processPaymentPort = processPaymentPort;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-event}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = "spring.json.value.default.type=com.salessystem.paymentservice.adapters.inbound.messaging.OrderEventDTO")
    public void consume(OrderEventDTO payload, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.info("=== [Payment Kafka Consumer] Event received from 'order-event' ===");
        log.info(" -> Order ID: {} | Event Type: {}", payload.getOrderId(), payload.getOrderEventType());

        if (payload.getOrderEventType() == null) {
            log.error(" -> Error: EventType is invalid in payload! Cannot route message.");
            throw new IllegalArgumentException("Invalid event type, check .DLT");
        }

        // Routing to the Domain Use Case through the Inbound Port
        switch (payload.getOrderEventType()) {
            case OrderEventType.ORDER_PLACED:
                log.info(" -> Action: Routing to process payment for Order ID: {}", payload.getOrderId());
                // At this point a real implementation should call the port to process the payment at external institution

                processPaymentPort.processPayment(
                    key,
                    payload.getOrderId(),
                    payload.getCustomerId(),
                    payload.getTotalAmount(),
                    payload.getPaymentToken()
                );

                log.info(" -> Processed payment placement event.");
            break;

            case OrderEventType.STOCK_DECLINED:
                log.info(" -> Action: Routing to process refund/compensation for Order ID: {}", payload.getOrderId());
                // At this point a real implementation should call the port to process the refund at external institution

                processPaymentPort.processRefund(key, payload.getOrderId());
                log.info(" -> Success: Domain processed cancellation/refund event.");
                break;

            case OrderEventType.PAYMENT_DECLINED:
                log.info("Payment declined event received from order-service, no action here, already processed - Order ID: {}", payload.getOrderId());
                break;

        }
    }
}