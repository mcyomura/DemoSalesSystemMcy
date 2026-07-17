package com.salessystem.orderservice.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@Sql(scripts = "/03 DB-ORDERS-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderStatusIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should successfully retrieve tracking status of an order after saga completion")
    void shouldRetrieveOrderStatusDetailsWithoutItems() throws Exception {
        // --- ARRANGE: Setup an approved order state directly in real MariaDB container ---
        String trackingCartUuid = UUID.randomUUID().toString();
        Integer orderId = 109;
        Integer customerId = 82732917;

        // Inserting the final state of a successful Saga orchestrator execution
        jdbcTemplate.execute(String.format(
                "INSERT INTO orders (id, uuid, customer_id, status, inventory_status, payment_status, total_amount, price_updated_at) " +
                        "VALUES (%s, '%s', %d, 'APPROVED', 'SUCCESS', 'SUCCESS', 59.80, NOW())",
                orderId, trackingCartUuid, customerId
        ));

        // --- ACT & ASSERT: Execute HTTP GET to fetch tracking status details ---
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.inventory_status").value("SUCCESS"))
                .andExpect(jsonPath("$.payment_status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalAmount").value(59.80))
                .andExpect(jsonPath("$.items").doesNotExist()); // Verification rule: Ensure items block is missing
    }
}