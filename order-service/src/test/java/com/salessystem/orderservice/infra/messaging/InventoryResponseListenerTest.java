package com.salessystem.orderservice.infra.messaging;

import com.salessystem.orderservice.domain.*;
import com.salessystem.orderservice.infra.messaging.dto.InventoryValidatedEventDTO;
import com.salessystem.orderservice.infra.messaging.dto.OrderEventDTO;
import com.salessystem.orderservice.infra.messaging.mapper.OrderEventMapper;
import com.salessystem.orderservice.infra.persistence.OrderRepositoryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryResponseListenerTest {

    @Mock
    private OrderRepositoryGateway orderRepositoryGtw;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OrderEventMapper orderEventMapper;

    private InventoryResponseListener inventoryResponseListener;

    private final String orderEventTopic = "order-events-topic";

    @BeforeEach
    void setUp() {
        inventoryResponseListener = new InventoryResponseListener(orderRepositoryGtw, kafkaTemplate, orderEventMapper);

        // Injecting the private topic name using ReflectionTestUtils
        ReflectionTestUtils.setField(inventoryResponseListener, "topicOrderEvent", orderEventTopic);
    }

    @Test
    @DisplayName("Should update inventory status to SUCCESS and save, but keep order status unchanged if payment is still pending")
    void shouldHandleInventorySuccessWithPaymentPending() {
        // Arrange
        Integer orderId = 123;
        InventoryValidatedEventDTO responseEvent = new InventoryValidatedEventDTO();
        responseEvent.setOrderId(orderId);
        responseEvent.setServiceStatus("SUCCESS");

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus(OrderStatus.PENDING);
        existingOrder.setInventoryStatus(SagaStatus.PENDING);
        existingOrder.setPaymentStatus(SagaStatus.PENDING); // Payment is not success yet

        when(orderRepositoryGtw.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Act
        inventoryResponseListener.consume(responseEvent);

        // Assert
        assertThat(existingOrder.getInventoryStatus()).isEqualTo(SagaStatus.SUCCESS);
        assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.PENDING); // Remains pending

        verify(orderRepositoryGtw, times(1)).save(existingOrder);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should approve order when inventory is SUCCESS and payment status is already SUCCESS")
    void shouldApproveOrderWhenBothInventoryAndPaymentAreSuccess() {
        // Arrange
        Integer orderId = 123;
        InventoryValidatedEventDTO responseEvent = new InventoryValidatedEventDTO();
        responseEvent.setOrderId(orderId);
        responseEvent.setServiceStatus("SUCCESS");

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus(OrderStatus.PENDING);
        existingOrder.setInventoryStatus(SagaStatus.PENDING);
        existingOrder.setPaymentStatus(SagaStatus.SUCCESS); // Payment already succeeded!

        when(orderRepositoryGtw.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Act
        inventoryResponseListener.consume(responseEvent);

        // Assert
        assertThat(existingOrder.getInventoryStatus()).isEqualTo(SagaStatus.SUCCESS);
        assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.APPROVED); // Approved!

        verify(orderRepositoryGtw, times(1)).save(existingOrder);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should cancel order and send a compensation event to Kafka when inventory validation FAILS")
    void shouldCancelOrderAndPublishCompensationEventWhenInventoryFails() {
        // Arrange
        Integer orderId = 123;
        String cartUuid = UUID.randomUUID().toString();

        InventoryValidatedEventDTO responseEvent = new InventoryValidatedEventDTO();
        responseEvent.setOrderId(orderId);
        responseEvent.setServiceStatus("FAILED");
        responseEvent.setReason("Out of stock");

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
        inventoryResponseListener.consume(responseEvent);

        // Assert
        assertThat(existingOrder.getInventoryStatus()).isEqualTo(SagaStatus.FAILED);
        assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED); // Cancelled due to failure

        verify(orderRepositoryGtw, times(1)).save(existingOrder);
        verify(orderEventMapper, times(1)).toEventDto(existingOrder);

        // Verify compensation event details
        assertThat(mockEventDto.getOrderEventType()).isEqualTo(OrderEventType.STOCK_DECLINED);

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
    @DisplayName("Should throw IllegalArgumentException when order is not found by ID")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        Integer invalidOrderId = 999;
        InventoryValidatedEventDTO responseEvent = new InventoryValidatedEventDTO();
        responseEvent.setOrderId(invalidOrderId);

        when(orderRepositoryGtw.findById(invalidOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryResponseListener.consume(responseEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found for ID: " + invalidOrderId);

        verify(orderRepositoryGtw, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should log error and do nothing when receiving an invalid or unexpected saga status from Inventory")
    void shouldDoNothingWhenInventorySendsInvalidStatus() {
        // Arrange
        Integer orderId = 123;
        InventoryValidatedEventDTO responseEvent = new InventoryValidatedEventDTO();
        responseEvent.setOrderId(orderId);
        responseEvent.setServiceStatus(SagaStatus.REFUNDED.name()); // Unexpected status for inventory validation stage

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus(OrderStatus.PENDING);
        existingOrder.setInventoryStatus(SagaStatus.PENDING);
        existingOrder.setPaymentStatus(SagaStatus.PENDING);

        when(orderRepositoryGtw.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Get the Logback Logger for the listener class
        Logger logger = (Logger) LoggerFactory.getLogger(InventoryResponseListener.class);

        // Create and start a ListAppender to capture log events in memory
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            // Act
            inventoryResponseListener.consume(responseEvent);

            // Assert
            // 1. Verify that the order was NEVER saved or updated on default case
            verify(orderRepositoryGtw, never()).save(any());
            verify(kafkaTemplate, never()).send(any(), any(), any());

            // 2. Verify that the expected error message was logged with ERROR level
            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(listAppender.list)
                        .isNotEmpty()
                        .anySatisfy(event -> {
                            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                            assertThat(event.getFormattedMessage()).contains("Invalid status from Inventory service");
                        });
            });

        } finally {
            // Clean up: stop the appender and remove it from the logger to avoid memory leaks
            listAppender.stop();
            logger.detachAppender(listAppender);
        }
    }
}