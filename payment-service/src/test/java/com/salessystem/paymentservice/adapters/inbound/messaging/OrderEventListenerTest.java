package com.salessystem.paymentservice.adapters.inbound.messaging;

import com.salessystem.paymentservice.ports.inbound.ProcessPaymentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private ProcessPaymentPort processPaymentPort;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private String kafkaKey;
    private Integer orderId;
    private Integer customerId;
    private BigDecimal totalAmount;
    private String paymentToken;

    @BeforeEach
    void setUp() {
        // Given: Common setup variables for the tests
        kafkaKey = "test-kafka-key-999";
        orderId = 55;
        customerId = 888;
        totalAmount = new BigDecimal("250.00");
        paymentToken = "TEST_TOKEN_123";
    }

    @Test
    @DisplayName("Should route to processPayment when event type is ORDER_PLACED")
    void shouldRouteToProcessPaymentWhenOrderPlaced() {
        // Given: An event DTO with type ORDER_PLACED
        OrderEventDTO payload = new OrderEventDTO(
                OrderEventType.ORDER_PLACED, orderId, customerId, totalAmount,
                LocalDateTime.now(), Collections.emptyList(), paymentToken
        );

        // When: Directly invoking the consume method (simulating Kafka trigger)
        orderEventListener.consume(payload, kafkaKey);

        // Then: Verify that the inbound port was called with exact parameters
        verify(processPaymentPort, times(1)).processPayment(
                kafkaKey, orderId, customerId, totalAmount, paymentToken
        );

        // Ensure refund was never called
        verify(processPaymentPort, never()).processRefund(any(), any());
    }

    @Test
    @DisplayName("Should route to processRefund when event type is STOCK_DECLINED")
    void shouldRouteToProcessRefundWhenStockDeclined() {
        // Given: An event DTO with type STOCK_DECLINED
        OrderEventDTO payload = new OrderEventDTO(
                OrderEventType.STOCK_DECLINED, orderId, customerId, totalAmount,
                LocalDateTime.now(), Collections.emptyList(), null
        );

        // When: Consuming the message
        orderEventListener.consume(payload, kafkaKey);

        // Then: Verify that the inbound port was called to process refund
        verify(processPaymentPort, times(1)).processRefund(kafkaKey, orderId);

        // Ensure payment placement was never called
        verify(processPaymentPort, never()).processPayment(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should take no action when event type is PAYMENT_DECLINED")
    void shouldTakeNoActionWhenPaymentDeclined() {
        // Given: An event DTO with type PAYMENT_DECLINED
        OrderEventDTO payload = new OrderEventDTO(
                OrderEventType.PAYMENT_DECLINED, orderId, customerId, totalAmount,
                LocalDateTime.now(), Collections.emptyList(), paymentToken
        );

        // When: Consuming the message
        orderEventListener.consume(payload, kafkaKey);

        // Then: Ensure absolutely no port methods were invoked
        verifyNoInteractions(processPaymentPort);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when event type is null to trigger DLQ")
    void shouldThrowExceptionWhenEventTypeIsNull() {
        // Given: An event DTO with a missing (null) event type
        OrderEventDTO payload = new OrderEventDTO(
                null, orderId, customerId, totalAmount,
                LocalDateTime.now(), Collections.emptyList(), paymentToken
        );

        // When & Then: Asserting that the method throws the expected exception
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        orderEventListener.consume(payload, kafkaKey)
                )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid event type, check .DLT");

        // Ensure the inbound port was never touched
        verifyNoInteractions(processPaymentPort);
    }
}