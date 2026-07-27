package com.salessystem.orderservice.infra.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salessystem.orderservice.application.exception.IllegalOrderStateException;
import com.salessystem.orderservice.application.exception.ResourceNotFoundException;
import com.salessystem.orderservice.application.usecase.ConsultOrderUseCase;
import com.salessystem.orderservice.application.usecase.ManageCartUseCase;
import com.salessystem.orderservice.application.usecase.OrderPlacedUseCase;
import com.salessystem.orderservice.domain.ManageCartResult;
import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.domain.OrderStatus;
import com.salessystem.orderservice.domain.SagaStatus;
import com.salessystem.orderservice.infra.web.dto.AddCartItemRequestDTO;
import com.salessystem.orderservice.infra.web.dto.CartResponseDTO;
import com.salessystem.orderservice.infra.web.dto.OrderPlacedRequestDTO;
import com.salessystem.orderservice.infra.web.mapper.OrderWebMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
// Injecting the path property dynamically so MockMvc maps the URL correctly
@TestPropertySource(properties = "app.api.path.orders=/api/v1/orders")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ManageCartUseCase manageCartUseCase;

    @MockitoBean
    private OrderPlacedUseCase orderPlacedUseCase;

    @MockitoBean
    private ConsultOrderUseCase consultOrderUseCase;

    @MockitoBean
    private OrderWebMapper orderWebMapper;

    @Test
    @DisplayName("Should return HTTP 201 Created and CartResponseDTO when adding item to cart successfully")
    void shouldAddItemToCartSuccessfully() throws Exception {
        // Arrange
        String cartUuid = UUID.randomUUID().toString();
        AddCartItemRequestDTO requestDTO = new AddCartItemRequestDTO(cartUuid, 123, 999, 2);

        Order domainOrder = new Order();
        domainOrder.setUuid(cartUuid);
        domainOrder.setCustomerId(123);
        domainOrder.setStatus(OrderStatus.DRAFT);
        domainOrder.setTotalAmount(new BigDecimal("100.00"));

        ManageCartResult useCaseResult = new ManageCartResult(domainOrder, false);

        CartResponseDTO expectedResponse = new CartResponseDTO();
        expectedResponse.setUuid(cartUuid);
        expectedResponse.setCustomerId(123);
        expectedResponse.setStatus("DRAFT");
        expectedResponse.setTotalAmount(new BigDecimal("100.00"));
        expectedResponse.setItems(new ArrayList<>());

        // Setting up behavior for mock beans
        when(manageCartUseCase.executeAddItem(eq(cartUuid), eq(123), eq(999), eq(2)))
                .thenReturn(useCaseResult);
        when(orderWebMapper.toResponse(any(Order.class)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(cartUuid))
                .andExpect(jsonPath("$.customerId").value(123))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.pricesUpdated").value(false));
    }

    @Test
    @DisplayName("Should return HTTP 200 OK and OrderStatusResponseDTO when checkout process is triggered")
    void shouldCheckoutOrderSuccessfully() throws Exception {
        // Arrange
        String cartUuid = UUID.randomUUID().toString();
        OrderPlacedRequestDTO requestDTO = new OrderPlacedRequestDTO(cartUuid, 123, "pay-token-xyz");

        Order updatedOrder = new Order();
        updatedOrder.setId(42);
        updatedOrder.setCustomerId(123);
        updatedOrder.setStatus(OrderStatus.PENDING);
        updatedOrder.setInventoryStatus(SagaStatus.PENDING);
        updatedOrder.setPaymentStatus(SagaStatus.PENDING);
        updatedOrder.setTotalAmount(new BigDecimal("250.00"));

        when(orderPlacedUseCase.execute(eq(cartUuid), eq(123), eq("pay-token-xyz")))
                .thenReturn(updatedOrder);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.customerId").value(123))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.inventory_status").value("PENDING"))
                .andExpect(jsonPath("$.payment_status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(250.00));
    }

    @Test
    @DisplayName("Should return HTTP 200 OK and OrderStatusResponseDTO when order is found by ID")
    void shouldReturnOrderWhenConsultedById() throws Exception {
        // Arrange
        Integer orderId = 42;
        Order foundOrder = new Order();
        foundOrder.setId(orderId);
        foundOrder.setCustomerId(123);
        foundOrder.setStatus(OrderStatus.APPROVED);
        foundOrder.setInventoryStatus(SagaStatus.SUCCESS);
        foundOrder.setPaymentStatus(SagaStatus.SUCCESS);
        foundOrder.setTotalAmount(new BigDecimal("75.00"));

        when(consultOrderUseCase.execute(orderId)).thenReturn(foundOrder);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.customerId").value(123))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.totalAmount").value(75.00));
    }

    @Test
    @DisplayName("Should return HTTP 404 Not Found when order is not found in database")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        // Arrange
        Integer invalidOrderId = 999;

        // Simulating the business Exception from the UseCase
        when(consultOrderUseCase.execute(invalidOrderId))
                .thenThrow(new ResourceNotFoundException("ID invalid:" + invalidOrderId));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/{id}", invalidOrderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Assumes GlobalExceptionHandler maps ResourceNotFoundException to 404
    }

    @Test
    @DisplayName("Should return HTTP 404 Not Found when adding item to a non-existent cart UUID")
    void shouldReturn404WhenAddingItemToInvalidCart() throws Exception {
        String invalidUuid = "invalid-uuid";
        AddCartItemRequestDTO requestDTO = new AddCartItemRequestDTO(invalidUuid, 123, 999, 2);

        // Simulating the exception from the UseCase
        when(manageCartUseCase.executeAddItem(eq(invalidUuid), eq(123), eq(999), eq(2)))
                .thenThrow(new ResourceNotFoundException("UUID not found:" + invalidUuid));

        mockMvc.perform(post("/api/v1/orders/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return HTTP 409 Conflict when trying to checkout an already closed cart")
    void shouldReturn400WhenCheckoutClosedCart() throws Exception {
        String cartUuid = UUID.randomUUID().toString();
        OrderPlacedRequestDTO requestDTO = new OrderPlacedRequestDTO(cartUuid, 123, "pay-token");

        // Simulating the illegal state exception
        when(orderPlacedUseCase.execute(eq(cartUuid), eq(123), any()))
                .thenThrow(new IllegalOrderStateException("Shopping cart already closed. UUID:" + cartUuid));

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isConflict());
    }

}