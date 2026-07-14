package com.salessystem.catalogservice.messaging;

import com.salessystem.catalogservice.dto.InventoryValidatedEventDTO;
import com.salessystem.catalogservice.dto.OrderEventDTO;
import com.salessystem.catalogservice.dto.OrderEventType;
import com.salessystem.catalogservice.dto.StockResult;
import com.salessystem.catalogservice.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPlacedListenerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderPlacedListener orderPlacedListener;

    private final String topicName = "inventory-validated-topic";
    private final String kafkaKey = "order-key-123";

    @BeforeEach
    void setUp() {
        // Explicitly inject the @Value property string into the listener using Reflection
        ReflectionTestUtils.setField(orderPlacedListener, "inventoryValidatedTopic", topicName);
    }

    @Test
    @DisplayName("Should deduct stock and send SUCCESS event when order is placed")
    void consume_OrderPlaced_Success() {
        // Arrange
        OrderEventDTO event = new OrderEventDTO();
        event.setOrderId(100);
        event.setOrderEventType(OrderEventType.ORDER_PLACED);

        // Act
        orderPlacedListener.consume(event, kafkaKey);

        // Assert
        verify(inventoryService, times(1)).deductStock(event);

        ArgumentCaptor<InventoryValidatedEventDTO> eventCaptor = ArgumentCaptor.forClass(InventoryValidatedEventDTO.class);
        verify(kafkaTemplate, times(1)).send(eq(topicName), eq(kafkaKey), eventCaptor.capture());

        InventoryValidatedEventDTO publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(100);
        assertThat(publishedEvent.getServiceStatus()).isEqualTo(StockResult.SUCCESS.name());
        assertThat(publishedEvent.getReason().contains("Stock successfully allocated"));
    }

    @Test
    @DisplayName("Should send FAILED event when stock deduction throws an exception")
    void consume_OrderPlaced_InsufficientStock_SendsFailedEvent() {
        // Arrange
        OrderEventDTO event = new OrderEventDTO();
        event.setOrderId(100);
        event.setOrderEventType(OrderEventType.ORDER_PLACED);

        String errorMessage = "Insufficient stock for Product ID: 5";
        doThrow(new IllegalStateException(errorMessage)).when(inventoryService).deductStock(event);

        // Act
        orderPlacedListener.consume(event, kafkaKey);

        // Assert
        verify(inventoryService, times(1)).deductStock(event);

        ArgumentCaptor<InventoryValidatedEventDTO> eventCaptor = ArgumentCaptor.forClass(InventoryValidatedEventDTO.class);
        verify(kafkaTemplate, times(1)).send(eq(topicName), eq(kafkaKey), eventCaptor.capture());

        InventoryValidatedEventDTO publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(100);
        assertThat(publishedEvent.getServiceStatus()).isEqualTo(StockResult.FAILED.name());
        assertThat(publishedEvent.getReason()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("Should return stock and send RETURNED event when payment is declined")
    void consume_PaymentDeclined_ReturnsStockSuccessfully() {
        // Arrange
        OrderEventDTO event = new OrderEventDTO();
        event.setOrderId(100);
        event.setOrderEventType(OrderEventType.PAYMENT_DECLINED);

        // Act
        orderPlacedListener.consume(event, kafkaKey);

        // Assert
        verify(inventoryService, times(1)).returnStock(event);
        verify(inventoryService, never()).deductStock(event);

        ArgumentCaptor<InventoryValidatedEventDTO> eventCaptor = ArgumentCaptor.forClass(InventoryValidatedEventDTO.class);
        verify(kafkaTemplate, times(1)).send(eq(topicName), eq(kafkaKey), eventCaptor.capture());

        InventoryValidatedEventDTO publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(100);
        assertThat(publishedEvent.getServiceStatus()).isEqualTo(StockResult.RETURNED.name());
        assertThat(publishedEvent.getReason()).contains("Items returned to stock");
    }

    @Test
    @DisplayName("Should do nothing when event type is STOCK_DECLINED")
    void consume_StockDeclined_DoesNothing() {
        // Arrange
        OrderEventDTO event = new OrderEventDTO();
        event.setOrderId(100);
        event.setOrderEventType(OrderEventType.STOCK_DECLINED);

        // Act
        orderPlacedListener.consume(event, kafkaKey);

        // Assert
        verifyNoInteractions(inventoryService);
        verifyNoInteractions(kafkaTemplate);
    }
}