package com.salessystem.orderservice.infra.messaging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.salessystem.orderservice.domain.*;
import com.salessystem.orderservice.infra.messaging.dto.OrderEventDTO;
import com.salessystem.orderservice.infra.messaging.dto.PaymentValidatedEventDTO;
import com.salessystem.orderservice.infra.messaging.mapper.OrderEventMapper;
import com.salessystem.orderservice.infra.persistence.OrderRepositoryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentResponseListenerTest {

    @Mock
    private OrderRepositoryGateway orderRepositoryGtw;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OrderEventMapper orderEventMapper;

    private PaymentResponseListener paymentResponseListener;

    private final String orderEventTopic = "order-event";

    @BeforeEach
    void setUp() {
        paymentResponseListener = new PaymentResponseListener(orderRepositoryGtw, kafkaTemplate, orderEventMapper);

        // Injecting the private topic name using ReflectionTestUtils
        ReflectionTestUtils.setField(paymentResponseListener, "topicOrderEvent", orderEventTopic);
    }

    @Test
    @DisplayName("Should update payment status to SUCCESS and save, but keep order status unchanged if inventory is still pending")
    void shouldHandlePaymentSuccessWithInventoryPending() {
        // Arrange
        Integer orderId = 123;
        PaymentValidatedEventDTO responseEvent = new PaymentValidatedEventDTO();
        responseEvent.setOrderId(orderId);
        responseEvent.setServiceStatus("SUCCESS");

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus(OrderStatus.PENDING);
        existingOrder.setInventoryStatus(SagaStatus.PENDING); // Stock is not validated yet
        existingOrder.setPaymentStatus(SagaStatus.PENDING);

        when(orderRepositoryGtw.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Act
        paymentResponseListener.consume(responseEvent);

        // Assert
        assertThat(existingOrder.getPaymentStatus()).isEqualTo(SagaStatus.SUCCESS);
        assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.PENDING); // Still pending

        verify(orderRepositoryGtw, times(1)).save(existingOrder);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should approve order when payment status is SUCCESS and inventory is already SUCCESS")
    void shouldApproveOrderWhenBothPaymentAndInventoryAreSuccess() {
        // Arrange
        Integer orderId = 123;
        PaymentValidatedEventDTO responseEvent = new PaymentValidatedEventDTO();
        responseEvent.setOrderId(orderId);
        responseEvent.setServiceStatus("SUCCESS");

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus(OrderStatus.PENDING);
        existingOrder.setInventoryStatus(SagaStatus.SUCCESS); // Inventory already success!
        existingOrder.setPaymentStatus(SagaStatus.PENDING);

        when(orderRepositoryGtw.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Act
        paymentResponseListener.consume(responseEvent);

        // Assert
        assertThat(existingOrder.getPaymentStatus()).isEqualTo(SagaStatus.SUCCESS);
        assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.APPROVED); // Order finalized successfully

        verify(orderRepositoryGtw, times(1)).save(existingOrder);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should cancel order and send a compensation event to Kafka when payment validation FAILS")
    void shouldCancelOrderAndPublishCompensationEventWhenPaymentFails() {
        // Arrange
        Integer orderId = 123;
        String cartUuid = UUID.randomUUID().toString();

        PaymentValidatedEventDTO responseEvent = new PaymentValidatedEventDTO();
        responseEvent.setOrderId(orderId);
        responseEvent.setServiceStatus("FAILED");
        responseEvent.setReason("Insufficient funds");

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setUuid(cartUuid);
        existingOrder.setStatus(OrderStatus.PENDING);
        existingOrder.setInventoryStatus(SagaStatus.PENDING);
        existingOrder.setPaymentStatus(SagaStatus.PENDING);

        OrderEventDTO mockEventDto = new OrderEventDTO();

        when(orderRepositoryGtw.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderEventMapper.toEventDto(existingOrder)).thenReturn(mockEventDto);

        // Act
        paymentResponseListener.consume(responseEvent);

        // Assert
        assertThat(existingOrder.getPaymentStatus()).isEqualTo(SagaStatus.FAILED);
        assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED); // Canceled because of payment error

        verify(orderRepositoryGtw, times(1)).save(existingOrder);
        verify(orderEventMapper, times(1)).toEventDto(existingOrder);

        // Verify captured compensation event details
        assertThat(mockEventDto.getOrderEventType()).isEqualTo(OrderEventType.PAYMENT_DECLINED);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate, times(1)).send(
                topicCaptor.capture(),
                keyCaptor.capture(),
                payloadCaptor.capture()
        );

        assertThat(topicCaptor.getValue()).isEqualTo(orderEventTopic);
        assertThat(keyCaptor.getValue()).isEqualTo(cartUuid);
        assertThat(payloadCaptor.getValue()).isEqualTo(mockEventDto);
    }

    @Test
    @DisplayName("Should handle payment REFUNDED status and save order to database")
    void shouldHandlePaymentRefundedStatus() {
        // Arrange
        Integer orderId = 123;
        PaymentValidatedEventDTO responseEvent = new PaymentValidatedEventDTO();
        responseEvent.setOrderId(orderId);
        responseEvent.setServiceStatus("REFUNDED");

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus(OrderStatus.PENDING);
        existingOrder.setInventoryStatus(SagaStatus.SUCCESS);
        existingOrder.setPaymentStatus(SagaStatus.PENDING);

        when(orderRepositoryGtw.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Act
        paymentResponseListener.consume(responseEvent);

        // Assert
        assertThat(existingOrder.getPaymentStatus()).isEqualTo(SagaStatus.REFUNDED);
        verify(orderRepositoryGtw, times(1)).save(existingOrder);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should log error and do nothing when receiving an unexpected status from Payment service")
    void shouldDoNothingWhenPaymentSendsInvalidStatus() {
        // Arrange
        Integer orderId = 123;
        PaymentValidatedEventDTO responseEvent = new PaymentValidatedEventDTO();
        responseEvent.setOrderId(orderId);
        responseEvent.setServiceStatus("DRAFT"); // Unexpected status for payment stage

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus(OrderStatus.PENDING);
        existingOrder.setInventoryStatus(SagaStatus.PENDING);
        existingOrder.setPaymentStatus(SagaStatus.PENDING);

        when(orderRepositoryGtw.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Get concrete Logback logger
        Logger logger = (Logger) LoggerFactory.getLogger(PaymentResponseListener.class);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            // Act
            paymentResponseListener.consume(responseEvent);

            // Assert
            verify(orderRepositoryGtw, never()).save(any());
            verify(kafkaTemplate, never()).send(any(), any(), any());

            // Synchronous assert on memory logs (no await required)
            assertThat(listAppender.list)
                    .isNotEmpty()
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                        assertThat(event.getFormattedMessage()).contains("Invalid status from Payment service");
                    });

        } finally {
            listAppender.stop();
            logger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when order is not found by ID")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        Integer invalidOrderId = 999;
        PaymentValidatedEventDTO responseEvent = new PaymentValidatedEventDTO();
        responseEvent.setOrderId(invalidOrderId);

        when(orderRepositoryGtw.findById(invalidOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentResponseListener.consume(responseEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found for ID: " + invalidOrderId);

        verify(orderRepositoryGtw, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}