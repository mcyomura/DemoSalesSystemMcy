package com.salessystem.orderservice.infra.messaging;

import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.domain.OrderEventType;
import com.salessystem.orderservice.infra.messaging.dto.OrderEventDTO;
import com.salessystem.orderservice.infra.messaging.mapper.OrderEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OrderEventMapper orderEventMapper;

    private OrderEventProducer orderEventProducer;

    private final String testTopic = "order-event";

    @BeforeEach
    void setUp() {
        orderEventProducer = new OrderEventProducer(kafkaTemplate, orderEventMapper);

        // Injecting the private @Value field using Spring's ReflectionTestUtils
        ReflectionTestUtils.setField(orderEventProducer, "topicName", testTopic);
    }

    @Test
    @DisplayName("Should successfully map, enrich and send order event to Kafka")
    void shouldSendOrderPlacedEventSuccessfully() {
        // Arrange
        String uuid = UUID.randomUUID().toString();
        String payToken = "payment_token_123";
        Order order = new Order(); // Empty domain object just for mapping reference

        OrderEventDTO mockEventDto = new OrderEventDTO(); // Assumed empty class or with basic setters

        when(orderEventMapper.toEventDto(order)).thenReturn(mockEventDto);

        // Act
        orderEventProducer.sendOrderPlacedEvent(order, uuid, payToken);

        // Assert
        // 1. Verify mapper was called
        verify(orderEventMapper, times(1)).toEventDto(order);

        // 2. Capture and verify the enrichment of the DTO
        assertThat(mockEventDto.getOrderEventType()).isEqualTo(OrderEventType.ORDER_PLACED);
        assertThat(mockEventDto.getPaymentToken()).isEqualTo(payToken);

        // 3. Capture the arguments passed to kafkaTemplate.send()
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate, times(1)).send(
                topicCaptor.capture(),
                keyCaptor.capture(),
                payloadCaptor.capture()
        );

        // 4. Assert Kafka message details
        assertThat(topicCaptor.getValue()).isEqualTo(testTopic);
        assertThat(keyCaptor.getValue()).isEqualTo(uuid);
        assertThat(payloadCaptor.getValue()).isEqualTo(mockEventDto);
    }
}