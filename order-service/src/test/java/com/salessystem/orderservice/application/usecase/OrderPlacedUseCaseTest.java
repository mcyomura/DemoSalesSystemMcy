package com.salessystem.orderservice.application.usecase;

import com.salessystem.orderservice.application.exception.IllegalOrderStateException;
import com.salessystem.orderservice.application.exception.ResourceNotFoundException;
import com.salessystem.orderservice.application.gateway.OrderGateway;
import com.salessystem.orderservice.application.gateway.OrderMessageGateway;
import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.domain.OrderStatus;
import com.salessystem.orderservice.domain.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPlacedUseCaseTest {

    @Mock
    private OrderGateway orderGateway;

    @Mock
    private OrderMessageGateway orderMessageGateway;

    private OrderPlacedUseCase orderPlacedUseCase;

    @BeforeEach
    void setUp() {
        orderPlacedUseCase = new OrderPlacedUseCase(orderGateway, orderMessageGateway);
    }

    @Test
    @DisplayName("Should successfully place an order, updating statuses to PENDING and sending a Kafka message")
    void shouldPlaceOrderSuccessfully() {
        // Arrange
        String cartUuid = UUID.randomUUID().toString();
        Integer customerId = 456;
        String payToken = "tok_visa_123";
        String bearerToken = "bearer-token-example";

        // Initial state of the order in DRAFT
        Order existingOrder = new Order();
        existingOrder.setUuid(cartUuid);
        existingOrder.setStatus(OrderStatus.DRAFT);
        existingOrder.setInventoryStatus(SagaStatus.DRAFT);
        existingOrder.setPaymentStatus(SagaStatus.DRAFT);

        when(orderGateway.findByUuid(cartUuid)).thenReturn(Optional.of(existingOrder));
        when(orderGateway.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderPlacedUseCase.execute(cartUuid, customerId, payToken);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getInventoryStatus()).isEqualTo(SagaStatus.PENDING);
        assertThat(result.getPaymentStatus()).isEqualTo(SagaStatus.PENDING);

        // Verify interaction with Database Gateway
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderGateway, times(1)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);

        // Verify interaction with Kafka Gateway (async notification)
        verify(orderMessageGateway, times(1)).sendOrderPlacedEvent(savedOrder, cartUuid, payToken);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when the cart UUID is not found")
    void shouldThrowExceptionWhenUuidNotFound() {
        // Arrange
        String nonExistentUuid = UUID.randomUUID().toString();
        when(orderGateway.findByUuid(nonExistentUuid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                orderPlacedUseCase.execute(nonExistentUuid, 456, "pay-token")
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UUID invalid:" + nonExistentUuid);

        verify(orderGateway, times(1)).findByUuid(nonExistentUuid);
        verify(orderGateway, never()).save(any());
        verify(orderMessageGateway, never()).sendOrderPlacedEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw IllegalOrderStateException when the cart status is not DRAFT")
    void shouldThrowExceptionWhenCartIsNotInDraft() {
        // Arrange
        String cartUuid = UUID.randomUUID().toString();

        // Cart is already PENDING (already placed)
        Order nonDraftOrder = new Order();
        nonDraftOrder.setUuid(cartUuid);
        nonDraftOrder.setStatus(OrderStatus.PENDING);

        when(orderGateway.findByUuid(cartUuid)).thenReturn(Optional.of(nonDraftOrder));

        // Act & Assert
        assertThatThrownBy(() ->
                orderPlacedUseCase.execute(cartUuid, 456, "pay-token")
        )
                .isInstanceOf(IllegalOrderStateException.class)
                .hasMessageContaining("Shopping cart already closed. UUID:" + cartUuid);

        verify(orderGateway, times(1)).findByUuid(cartUuid);
        verify(orderGateway, never()).save(any());
        verify(orderMessageGateway, never()).sendOrderPlacedEvent(any(), any(), any());
    }
}