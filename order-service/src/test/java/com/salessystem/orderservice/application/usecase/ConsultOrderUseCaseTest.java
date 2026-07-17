package com.salessystem.orderservice.application.usecase;

import com.salessystem.orderservice.application.exception.ResourceNotFoundException;
import com.salessystem.orderservice.application.gateway.OrderGateway;
import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.domain.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultOrderUseCaseTest {

    @Mock
    private OrderGateway orderGateway;

    private ConsultOrderUseCase consultOrderUseCase;

    @BeforeEach
    void setUp() {
        consultOrderUseCase = new ConsultOrderUseCase(orderGateway);
    }

    @Test
    @DisplayName("Should return the order when a valid ID is provided")
    void shouldReturnOrderWhenIdExists() {
        // Arrange
        Integer existingId = 1;
        Order mockOrder = new Order();
        mockOrder.setId(existingId);
        mockOrder.setStatus(OrderStatus.DRAFT);
        mockOrder.setTotalAmount(new BigDecimal("150.00"));

        when(orderGateway.findById(existingId)).thenReturn(Optional.of(mockOrder));

        // Act
        Order result = consultOrderUseCase.execute(existingId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existingId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("150.00"));

        verify(orderGateway, times(1)).findById(existingId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when the provided ID does not exist")
    void shouldThrowExceptionWhenIdDoesNotExist() {
        // Arrange
        Integer nonExistentId = 999;
        when(orderGateway.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> consultOrderUseCase.execute(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ID invalid:" + nonExistentId);

        verify(orderGateway, times(1)).findById(nonExistentId);
    }
}