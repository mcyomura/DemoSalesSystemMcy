package com.salessystem.orderservice.application.usecase;

import com.salessystem.orderservice.application.exception.IllegalOrderStateException;
import com.salessystem.orderservice.application.exception.ResourceNotFoundException;
import com.salessystem.orderservice.application.gateway.OrderGateway;
import com.salessystem.orderservice.application.gateway.ProductGateway;
import com.salessystem.orderservice.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageCartUseCaseTest {

    @Mock
    private OrderGateway orderGateway;

    @Mock
    private ProductGateway productGateway;

    private ManageCartUseCase manageCartUseCase;

    // Configured stale limit set to 15 minutes for testing
    private final long cartRefreshMinutes = 15;

    @BeforeEach
    void setUp() {
        manageCartUseCase = new ManageCartUseCase(orderGateway, productGateway, cartRefreshMinutes);
    }

    @Test
    @DisplayName("Should successfully create a brand new cart and add an item when cartUuid is null")
    void shouldCreateNewCartAndAddItemWhenUuidIsNull() {
        // Arrange
        Integer customerId = 123;
        Integer productId = 999;
        Integer quantity = 2;
        BigDecimal productPrice = new BigDecimal("50.01");

        Product productResponse = new Product(productId,"Test Product", productPrice );

        when(productGateway.getProductById(productId)).thenReturn(productResponse);

        // Mocking saving behavior to return the same order passed to it
        when(orderGateway.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ManageCartResult result = manageCartUseCase.executeAddItem(null, customerId, productId, quantity);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.pricesUpdated()).isFalse();

        Order savedOrder = result.order();
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getUuid()).isNotNull();
        assertThat(savedOrder.getCustomerId()).isEqualTo(customerId);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.02")); // 50.01 * 2

        verify(productGateway, times(1)).getProductById(productId);
        verify(orderGateway, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should add item to an existing cart without refreshing prices if prices are not stale")
    void shouldAddItemToExistingCartWithoutRefreshWhenPricesAreNotStale() {
        // Arrange
        String cartUuid = UUID.randomUUID().toString();
        Integer customerId = 123;
        Integer productId = 888;
        Integer quantity = 1;
        BigDecimal newProductPrice = new BigDecimal("30.02");

        // Existing order state (last updated 5 minutes ago -> not stale)
        Order existingOrder = new Order();
        existingOrder.setUuid(cartUuid);
        existingOrder.setStatus(OrderStatus.DRAFT);
        existingOrder.setPriceUpdatedAt(LocalDateTime.now().minusMinutes(5));
        existingOrder.setTotalAmount(new BigDecimal("150.00"));
        existingOrder.setItems(new ArrayList<>()); // Assuming getter returns a modifiable list

        Product productResponse = new Product(productId,"Test Product", newProductPrice );

        when(orderGateway.findByUuid(cartUuid)).thenReturn(Optional.of(existingOrder));
        when(productGateway.getProductById(productId)).thenReturn(productResponse);
        when(orderGateway.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ManageCartResult result = manageCartUseCase.executeAddItem(cartUuid, customerId, productId, quantity);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.pricesUpdated()).isFalse();

        Order savedOrder = result.order();
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("180.02")); // 150.00 + 30.02
        assertThat(savedOrder.getCustomerId()).isEqualTo(customerId);

        verify(orderGateway, times(1)).findByUuid(cartUuid);
        verify(productGateway, times(1)).getProductById(productId);
        verify(orderGateway, times(1)).save(savedOrder);
    }

    @Test
    @DisplayName("Should refresh stale prices of existing items and add new item when price state is expired")
    void shouldRefreshPricesAndAddItemWhenPricesAreStale() {
        // Arrange
        String cartUuid = UUID.randomUUID().toString();
        Integer customerId = 123;
        Integer existingProductId = 111;
        Integer newProductId = 222;

        // Existing order state (last updated 30 minutes ago -> stale)
        Order existingOrder = new Order();
        existingOrder.setUuid(cartUuid);
        existingOrder.setStatus(OrderStatus.DRAFT);
        existingOrder.setPriceUpdatedAt(LocalDateTime.now().minusMinutes(30));
        existingOrder.setTotalAmount(new BigDecimal("100.00")); // Old total from old price

        OrderItem existingItem = new OrderItem();
        existingItem.setProductId(existingProductId);
        existingItem.setQuantity(2);
        existingItem.setPriceAtPurchase(new BigDecimal("50.00")); // Old price
        existingOrder.getItems().add(existingItem);

        // Catalog prices (existing product decreased from 50.00 to 40.00)
        Product existingProductResponse = new Product(existingProductId,"Test Product", new BigDecimal("40.00"));

        // New product price
        Product newProductResponse = new Product(newProductId,"Test Product", new BigDecimal("35.00"));

        when(orderGateway.findByUuid(cartUuid)).thenReturn(Optional.of(existingOrder));
        when(productGateway.getProductById(existingProductId)).thenReturn(existingProductResponse);
        when(productGateway.getProductById(newProductId)).thenReturn(newProductResponse);
        when(orderGateway.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ManageCartResult result = manageCartUseCase.executeAddItem(cartUuid, customerId, newProductId, 1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.pricesUpdated()).isTrue();

        Order savedOrder = result.order();
        // Calculation: (2 * 40.00 [refreshed]) + (1 * 35.00 [new]) = 80.00 + 35.00 = 115.00
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("115.00"));
        assertThat(existingItem.getPriceAtPurchase()).isEqualByComparingTo(new BigDecimal("40.00"));

        verify(productGateway, times(1)).getProductById(existingProductId);
        verify(productGateway, times(1)).getProductById(newProductId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when cart UUID does not exist")
    void shouldThrowExceptionWhenCartUuidNotFound() {
        // Arrange
        String nonExistentUuid = "non-existent-uuid";
        when(orderGateway.findByUuid(nonExistentUuid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                manageCartUseCase.executeAddItem(nonExistentUuid, 123, 999, 1)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UUID not found:" + nonExistentUuid);

        verify(orderGateway, times(1)).findByUuid(nonExistentUuid);
        verify(productGateway, never()).getProductById(any());
        verify(orderGateway, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalOrderStateException when cart status is not DRAFT")
    void shouldThrowExceptionWhenCartIsNotInDraftStatus() {
        // Arrange
        String cartUuid = UUID.randomUUID().toString();
        Order closedOrder = new Order();
        closedOrder.setUuid(cartUuid);
        closedOrder.setStatus(OrderStatus.PENDING); // Non-draft status

        when(orderGateway.findByUuid(cartUuid)).thenReturn(Optional.of(closedOrder));

        // Act & Assert
        assertThatThrownBy(() ->
                manageCartUseCase.executeAddItem(cartUuid, 123, 999, 1)
        )
                .isInstanceOf(IllegalOrderStateException.class)
                .hasMessageContaining("Shopping cart already closed. UUID:" + cartUuid);

        verify(orderGateway, times(1)).findByUuid(cartUuid);
        verify(productGateway, never()).getProductById(any());
        verify(orderGateway, never()).save(any());
    }
}