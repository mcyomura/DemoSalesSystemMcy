package com.salessystem.paymentservice.adapters.outbound.messaging;

import com.salessystem.paymentservice.domain.model.PaymentValidatedEventDTO;
import com.salessystem.paymentservice.ports.outbound.PaymentEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaPaymentEventPublisherAdapter implements PaymentEventPublisherPort {
    @Value("${app.kafka.topics.payment-processed}")
    private String topicPayEvent;

    private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisherAdapter.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaPaymentEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(PaymentValidatedEventDTO event, String orderUuid) {
        log.info("=== [Payment Outbound Adapter] Publishing event to 'payment-event' ===");
        log.info(" -> Order ID: {} | Status Result: {}", event.getOrderId(), event.getServiceStatus().toString());

        // Publishing to Kafka using orderUuid as the partition KEY
        kafkaTemplate.send(topicPayEvent, orderUuid, event);

        log.info(" -> Success: Message successfully sent to Kafka with Key: {}", orderUuid);
    }
}