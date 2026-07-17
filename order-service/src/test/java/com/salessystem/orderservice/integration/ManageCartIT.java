package com.salessystem.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salessystem.orderservice.application.gateway.ProductGateway;
import com.salessystem.orderservice.domain.Product;
import com.salessystem.orderservice.infra.web.dto.AddCartItemRequestDTO; // Adjust import to your exact DTO package
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Standard for Spring Boot 3.4+ / 4.0
// If the import above fails in your setup, swap it for: import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@Sql(scripts = "/03 DB-ORDERS-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ManageCartIT extends BaseIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // We mock only the external catalog gateway to bypass network calls to catalog-service
    @MockitoBean
    private ProductGateway productGateway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should create a new shopping cart in MariaDB when adding the first item via HTTP POST")
    void shouldCreateCartAndAddItemSuccessfully() throws Exception {
        // Arrange
        Integer customerId = 555;
        Integer productId = 202;
        Integer quantity = 3;
        BigDecimal productPrice = new BigDecimal("15.50");

        // Preparing the payload using your controller request DTO pattern
        // Assumes constructor format: AddCartItemRequestDTO(cartUuid, customerId, productId, quantity)
        AddCartItemRequestDTO requestDto = new AddCartItemRequestDTO(null, customerId, productId, quantity);

        // Stubbing the clean product gateway to return the catalog information
        Product mockProduct = new Product(productId, "Integration Test Product", productPrice);
        when(productGateway.getProductById(productId)).thenReturn(mockProduct);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").isNotEmpty()) // Verifies Spring generated the business UUID
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(46.50)); // 3 items * 15.50 = 46.50
    }

    @Test
    @DisplayName("Should update existing shopping cart total amount when adding a second item via HTTP POST")
    void shouldAddSecondItemToExistingCartSuccessfully() throws Exception {
        // --- ARRANGE: Scenario Data Setup ---
        Integer customerId = 556;
        Integer firstProductId = 203;
        Integer secondProductId = 304;

        BigDecimal firstProductPrice = new BigDecimal("10.00");
        BigDecimal secondProductPrice = new BigDecimal("25.00");

        // Stubbing the product gateway for both unique products
        Product firstMockProduct = new Product(firstProductId, "First Product", firstProductPrice);
        Product secondMockProduct = new Product(secondProductId, "Second Product", secondProductPrice);

        when(productGateway.getProductById(firstProductId)).thenReturn(firstMockProduct);
        when(productGateway.getProductById(secondProductId)).thenReturn(secondMockProduct);

        // --- ACT & ASSERT: First Call (Create Cart with 2 units of First Product) ---
        AddCartItemRequestDTO firstRequest = new AddCartItemRequestDTO(null, customerId, firstProductId, 2);

        String firstResponseJson = mockMvc.perform(post("/api/v1/orders/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").isNotEmpty())
                .andExpect(jsonPath("$.totalAmount").value(20.00)) // 2 * 10.00 = 20.00
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the generated business UUID from the first HTTP response payload
        String generatedCartUuid = objectMapper.readTree(firstResponseJson).get("uuid").asText();

        // --- ACT & ASSERT: Second Call (Add 1 unit of Second Product to the same UUID) ---
        AddCartItemRequestDTO secondRequest = new AddCartItemRequestDTO(generatedCartUuid, customerId, secondProductId, 1);

        mockMvc.perform(post("/api/v1/orders/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(generatedCartUuid)) // Must be the same cart instance
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.totalAmount").value(45.00)); // 20.00 (previous) + (1 * 25.00) = 45.00
    }

    @Test
    @DisplayName("Should return 404 Not Found from GlobalExceptionHandler when UUID does not exist")
    void shouldReturn404FromGlobalHandlerWhenUuidNotFound() throws Exception {
        // --- ARRANGE ---
        Integer customerId = 557;
        Integer productId = 202;
        Integer quantity = 1;
        String nonExistingUuid = UUID.randomUUID().toString();

        AddCartItemRequestDTO requestDto = new AddCartItemRequestDTO(nonExistingUuid, customerId, productId, quantity);

        // --- ACT & ASSERT ---
        String responseJson = mockMvc.perform(post("/api/v1/orders/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseJson).contains("UUID not found");
    }

    @Test
    @DisplayName("Should return 409 Conflict from GlobalExceptionHandler when shopping cart is already closed")
    void shouldReturn409FromGlobalHandlerWhenCartIsClosed() throws Exception {
        // --- ARRANGE ---
        Integer customerId = 558;
        Integer productId = 202;
        Integer quantity = 1;
        String closedCartUuid = UUID.randomUUID().toString();

        // Step 1: Insert a closed order directly into the database container using a native query or spring template
        // To make this integration test realistic, we insert an order with status 'APPROVED' (Closed)
        // Adjust column names if they slightly differ from your schema definitions
        jdbcTemplate.execute(String.format(
                "INSERT INTO orders (uuid, customer_id, status, total_amount, price_updated_at) " +
                        "VALUES ('%s', %d, 'APPROVED', 150.00, NOW())",
                closedCartUuid, customerId
        ));

        // Stubbing the external catalog service product info
        Product mockProduct = new Product(productId, "Closed Cart Test Product", new BigDecimal("50.00"));
        when(productGateway.getProductById(productId)).thenReturn(mockProduct);

        AddCartItemRequestDTO requestDto = new AddCartItemRequestDTO(closedCartUuid, customerId, productId, quantity);

        // --- ACT & ASSERT ---
        String responseJson = mockMvc.perform(post("/api/v1/orders/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseJson).contains("Shopping cart already closed");
    }
}