package com.salessystem.catalogservice.service;

import com.salessystem.catalogservice.dto.OrderEventDTO;
import com.salessystem.catalogservice.model.Inventory;
import com.salessystem.catalogservice.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("Should successfully deduct stock when quantity is available")
    void deductStock_Success() {
        // Arrange
        Integer productId1 = 1001;
        int currentStock1 = 10;
        int requestedQuantity1 = 3;

        Integer productId2 = 1002;
        int currentStock2 = 25;
        int requestedQuantity2 = 7;

        Integer productId3 = 1003;
        int currentStock3 = 71;
        int requestedQuantity3 = 4;

        // 1. Create the DTO for the event containing 3 products
        OrderEventDTO event = new OrderEventDTO();

        // Include the list of products
        OrderEventDTO.OrderItemEventDTO itemEvent1 = new OrderEventDTO.OrderItemEventDTO();
        itemEvent1.setProductId(productId1);
        itemEvent1.setQuantity(requestedQuantity1);

        OrderEventDTO.OrderItemEventDTO itemEvent2 = new OrderEventDTO.OrderItemEventDTO();
        itemEvent2.setProductId(productId2);
        itemEvent2.setQuantity(requestedQuantity2);

        OrderEventDTO.OrderItemEventDTO itemEvent3 = new OrderEventDTO.OrderItemEventDTO();
        itemEvent3.setProductId(productId3);
        itemEvent3.setQuantity(requestedQuantity3);

        event.setItems(List.of(itemEvent1, itemEvent2, itemEvent3));

        // 2. Create the correspondent products in the "database" (model class)
        Inventory inventory1 = new Inventory();
        ReflectionTestUtils.setField(inventory1, "id", productId1); // Use Spring's ReflectionTestUtils to inject the private ID field without a setter
        inventory1.setQuantity(currentStock1);

        Inventory inventory2 = new Inventory();
        ReflectionTestUtils.setField(inventory2, "id", productId2); // Use Spring's ReflectionTestUtils to inject the private ID field without a setter
        inventory2.setQuantity(currentStock2);

        Inventory inventory3 = new Inventory();
        ReflectionTestUtils.setField(inventory3, "id", productId3); // Use Spring's ReflectionTestUtils to inject the private ID field without a setter
        inventory3.setQuantity(currentStock3);

        when(inventoryRepository.findByProductId(productId1)).thenReturn(Optional.of(inventory1));
        when(inventoryRepository.findByProductId(productId2)).thenReturn(Optional.of(inventory2));
        when(inventoryRepository.findByProductId(productId3)).thenReturn(Optional.of(inventory3));

        // Act
        inventoryService.deductStock(event);

        // Assert
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        // Verified that save was called 3 times (once per item)
        verify(inventoryRepository, times(3)).save(inventoryCaptor.capture());

        // Retrieve all captured items in order of invocation
        List<Inventory> savedInventories = inventoryCaptor.getAllValues();

        // Assert product 1 (Index 0)
        assertThat(savedInventories.get(0).getId()).isEqualTo(productId1);
        assertThat(savedInventories.get(0).getQuantity()).isEqualTo(7); // 10 - 3

        // Assert product 2 (Index 1)
        assertThat(savedInventories.get(1).getId()).isEqualTo(productId2);
        assertThat(savedInventories.get(1).getQuantity()).isEqualTo(18); // 25 - 7

        // Assert product 3 (Index 2)
        assertThat(savedInventories.get(2).getId()).isEqualTo(productId3);
        assertThat(savedInventories.get(2).getQuantity()).isEqualTo(67); // 71 - 4
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when product inventory is not found during deduction")
    void deductStock_ProductNotFound_ThrowsException() {
        // Arrange
        Integer productId = 1002;

        OrderEventDTO.OrderItemEventDTO itemEvent = new OrderEventDTO.OrderItemEventDTO();
        itemEvent.setProductId(productId);
        itemEvent.setQuantity(2);

        OrderEventDTO event = new OrderEventDTO();
        event.setItems(List.of(itemEvent));

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.deductStock(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock registry not found for Product ID: " + productId);

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when inventory stock is insufficient")
    void deductStock_InsufficientStock_ThrowsException() {
        // Arrange
        Integer productId = 1003;
        int currentStock = 2;
        int requestedQuantity = 5;

        OrderEventDTO.OrderItemEventDTO itemEvent = new OrderEventDTO.OrderItemEventDTO();
        itemEvent.setProductId(productId);
        itemEvent.setQuantity(requestedQuantity);

        OrderEventDTO event = new OrderEventDTO();
        event.setItems(List.of(itemEvent));

        Inventory inventory = new Inventory();
        ReflectionTestUtils.setField(inventory, "id", productId);
        inventory.setQuantity(currentStock);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.deductStock(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock for Product ID: "+productId);

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should successfully return items to stock")
    void returnStock_Success() {
        // Arrange
        Integer productId1 = 1001;
        int currentStock1 = 10;
        int returnedQuantity1 = 3;

        Integer productId2 = 1002;
        int currentStock2 = 22;
        int returnedQuantity2 = 7;

        Integer productId3 = 1003;
        int currentStock3 = 64;
        int returnedQuantity3 = 4;

        // 1. Create the DTO for the event containing 3 products
        OrderEventDTO event = new OrderEventDTO();

        // Include the list of products
        OrderEventDTO.OrderItemEventDTO itemEvent1 = new OrderEventDTO.OrderItemEventDTO();
        itemEvent1.setProductId(productId1);
        itemEvent1.setQuantity(returnedQuantity1);

        OrderEventDTO.OrderItemEventDTO itemEvent2 = new OrderEventDTO.OrderItemEventDTO();
        itemEvent2.setProductId(productId2);
        itemEvent2.setQuantity(returnedQuantity2);

        OrderEventDTO.OrderItemEventDTO itemEvent3 = new OrderEventDTO.OrderItemEventDTO();
        itemEvent3.setProductId(productId3);
        itemEvent3.setQuantity(returnedQuantity3);

        event.setItems(List.of(itemEvent1, itemEvent2, itemEvent3));

        // 2. Create the correspondent products in the "database" (model class)
        Inventory inventory1 = new Inventory();
        ReflectionTestUtils.setField(inventory1, "id", productId1); // Use Spring's ReflectionTestUtils to inject the private ID field without a setter
        inventory1.setQuantity(currentStock1);

        Inventory inventory2 = new Inventory();
        ReflectionTestUtils.setField(inventory2, "id", productId2); // Use Spring's ReflectionTestUtils to inject the private ID field without a setter
        inventory2.setQuantity(currentStock2);

        Inventory inventory3 = new Inventory();
        ReflectionTestUtils.setField(inventory3, "id", productId3); // Use Spring's ReflectionTestUtils to inject the private ID field without a setter
        inventory3.setQuantity(currentStock3);

        when(inventoryRepository.findByProductId(productId1)).thenReturn(Optional.of(inventory1));
        when(inventoryRepository.findByProductId(productId2)).thenReturn(Optional.of(inventory2));
        when(inventoryRepository.findByProductId(productId3)).thenReturn(Optional.of(inventory3));

        // Act
        inventoryService.returnStock(event);

        // Assert
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        // Verified that save was called 3 times (once per item)
        verify(inventoryRepository, times(3)).save(inventoryCaptor.capture());

        // Retrieve all captured items in order of invocation
        List<Inventory> savedInventories = inventoryCaptor.getAllValues();

        // Assert product 1 (Index 0)
        assertThat(savedInventories.get(0).getId()).isEqualTo(productId1);
        assertThat(savedInventories.get(0).getQuantity()).isEqualTo(13); // 10 + 3

        // Assert product 2 (Index 1)
        assertThat(savedInventories.get(1).getId()).isEqualTo(productId2);
        assertThat(savedInventories.get(1).getQuantity()).isEqualTo(29); // 22 + 7

        // Assert product 3 (Index 2)
        assertThat(savedInventories.get(2).getId()).isEqualTo(productId3);
        assertThat(savedInventories.get(2).getQuantity()).isEqualTo(68); // 64 + 4
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when product inventory is not found during return")
    void returnStock_ProductNotFound_ThrowsException() {
        // Arrange
        Integer productId = 1005;

        OrderEventDTO.OrderItemEventDTO itemEvent = new OrderEventDTO.OrderItemEventDTO();
        itemEvent.setProductId(productId);
        itemEvent.setQuantity(5);

        OrderEventDTO event = new OrderEventDTO();
        event.setItems(List.of(itemEvent));

        // Mocking the repository to return an empty Optional, simulating product not found
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.returnStock(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock registry not found for Product ID: " + productId);

        // Verify that save was never called since the exception stops the flow
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when product inventory is not found during return - 2nd item")
    void returnStock_ProductNotFoundItem2_ThrowsException() {
        Integer productId1 = 1006;
        int currentStock1 = 10;
        int requestedQuantity1 = 3;

        Integer productId2 = 1007;
        int currentStock2 = 25;
        int requestedQuantity2 = 7;

        Integer productId3 = 1008;
        int currentStock3 = 71;
        int requestedQuantity3 = 4;

        // 1. Create the DTO for the event containing 3 products
        OrderEventDTO event = new OrderEventDTO();

        // Include the list of products
        OrderEventDTO.OrderItemEventDTO itemEvent1 = new OrderEventDTO.OrderItemEventDTO();
        itemEvent1.setProductId(productId1);
        itemEvent1.setQuantity(requestedQuantity1);

        OrderEventDTO.OrderItemEventDTO itemEvent2 = new OrderEventDTO.OrderItemEventDTO();
        itemEvent2.setProductId(productId2);
        itemEvent2.setQuantity(requestedQuantity2);

        OrderEventDTO.OrderItemEventDTO itemEvent3 = new OrderEventDTO.OrderItemEventDTO();
        itemEvent3.setProductId(productId3);
        itemEvent3.setQuantity(requestedQuantity3);

        event.setItems(List.of(itemEvent1, itemEvent2, itemEvent3));

        // 2. Create the correspondent products in the "database" (model class), except the 2nd
        Inventory inventory1 = new Inventory();
        ReflectionTestUtils.setField(inventory1, "id", productId1); // Use Spring's ReflectionTestUtils to inject the private ID field without a setter
        inventory1.setQuantity(currentStock1);

        Inventory inventory3 = new Inventory();
        ReflectionTestUtils.setField(inventory3, "id", productId3); // Use Spring's ReflectionTestUtils to inject the private ID field without a setter
        inventory3.setQuantity(currentStock3);

        // Mocking the repository to return an empty Optional, simulating product not found
        when(inventoryRepository.findByProductId(productId1)).thenReturn(Optional.of(inventory1));
        when(inventoryRepository.findByProductId(productId2)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductId(productId1)).thenReturn(Optional.of(inventory3));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.returnStock(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock registry not found for Product ID: " + productId2);

        // Verify that the flow was called once
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(1)).save(inventoryCaptor.capture());
    }


}