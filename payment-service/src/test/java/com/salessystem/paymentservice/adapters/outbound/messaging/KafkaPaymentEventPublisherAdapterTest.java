package com.salessystem.paymentservice.adapters.outbound.messaging;

import com.salessystem.paymentservice.domain.model.PaymentValidatedEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaPaymentEventPublisherAdapterTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaPaymentEventPublisherAdapter adapter;

    private String topicName;
    private String orderUuid;
    private PaymentValidatedEventDTO eventDto;

    @BeforeEach
    void setUp() {
        // Given: Setting up variables and mock event DTO
        topicName = "mock-payment-processed-topic";
        orderUuid = "kafka-partition-key-uuid-123";
        eventDto = new PaymentValidatedEventDTO(1, "SUCCESS", "Payment processed successfully");

        // Spring utility to inject value into the private @Value annotated field without starting Spring context
        ReflectionTestUtils.setField(adapter, "topicPayEvent", topicName);
    }

    @Test
    @DisplayName("Should successfully send event payload to Kafka template with correct topic and key")
    void shouldPublishEventToKafkaSuccessfully() {
        // When: Triggering the outbound publishing adapter
        adapter.publish(eventDto, orderUuid);

        // Then: Verify that the KafkaTemplate was called with the right topic, key, and payload
        verify(kafkaTemplate, times(1)).send(topicName, orderUuid, eventDto);
    }
}