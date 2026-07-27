package com.salessystem.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.salessystem.bff.client.OrderClient;
import com.salessystem.bff.dto.cart.AddItemRequestDTO;
import com.salessystem.bff.dto.cart.CartResponseDTO;
import com.salessystem.bff.dto.cart.CheckoutRequestDTO;
import com.salessystem.bff.dto.cart.OrderStatusResponseDTO;
import com.salessystem.bff.service.JwtValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartBffController.class)
@TestPropertySource(properties = "app.api.path.bff=/api/v1/salesbff")
class CartBffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderClient orderClient;

    @MockitoBean
    private JwtValidationService jwtValidationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should return 210 Created when adding an item to the cart")
    void addItemToCart_ShouldReturnCreated() throws Exception {
        // Arrange: Prepare request and mock response DTOs
        AddItemRequestDTO request = new AddItemRequestDTO();
        // Assuming setters or field values can be populated if required

        CartResponseDTO mockResponse = new CartResponseDTO();

        given(orderClient.addItemToCart(any(AddItemRequestDTO.class))).willReturn(mockResponse);

        // Act & Assert: Perform POST and expect HTTP 201 Created
        mockMvc.perform(post("/api/v1/salesbff/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return 202 Accepted when checkout is triggered with valid Authorization header")
    void checkout_ShouldReturnAccepted() throws Exception {
        // Mock default behavior for valid JWT token validation
        JWTClaimsSet mockClaims = new JWTClaimsSet.Builder()
                .subject("12345")
                .build();
        given(jwtValidationService.validateToken(anyString())).willReturn(mockClaims);

        // Arrange: Prepare checkout payload
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        OrderStatusResponseDTO mockResponse = new OrderStatusResponseDTO();

        given(orderClient.confirmCheckout(any(CheckoutRequestDTO.class))).willReturn(mockResponse);

        // Act & Assert: Perform POST with Bearer token and expect HTTP 202 Accepted
        mockMvc.perform(post("/api/v1/salesbff/cart/checkout")
                        .header("Authorization", "Bearer valid_jwt_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Should return 202 Accepted when consulting an existing order with valid Authorization header")
    void consultOrder_ShouldReturnAccepted() throws Exception {
        // Mock default behavior for valid JWT token validation
        JWTClaimsSet mockClaims = new JWTClaimsSet.Builder()
                .subject("12345")
                .build();
        given(jwtValidationService.validateToken(anyString())).willReturn(mockClaims);

        // Arrange: Mocking order consultation return payload
        Integer orderId = 123;
        OrderStatusResponseDTO mockResponse = new OrderStatusResponseDTO();

        given(orderClient.consultOrder(orderId)).willReturn(mockResponse);

        // Act & Assert: Perform GET with Bearer token for the path variable and expect HTTP 202 Accepted
        mockMvc.perform(get("/api/v1/salesbff/cart/{id}", orderId)
                        .header("Authorization", "Bearer valid_jwt_token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when Authorization header is missing on protected endpoint")
    void checkout_ShouldReturnUnauthorizedWhenHeaderIsMissing() throws Exception {
        // Arrange
        CheckoutRequestDTO request = new CheckoutRequestDTO();

        // Act & Assert: Perform POST without Authorization header and expect HTTP 401 Unauthorized
        mockMvc.perform(post("/api/v1/salesbff/cart/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}