package com.salessystem.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salessystem.orderservice.application.gateway.ProductGateway;
import com.salessystem.orderservice.domain.OrderEventType;
import com.salessystem.orderservice.domain.OrderStatus;
import com.salessystem.orderservice.domain.SagaStatus;
import com.salessystem.orderservice.infra.messaging.dto.InventoryValidatedEventDTO;
import com.salessystem.orderservice.infra.messaging.dto.PaymentValidatedEventDTO;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(scripts = "/03 DB-ORDERS-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderCheckoutSagaIT extends BaseIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private ProductGateway productGateway;

    @Value("${app.kafka.topics.order-event}")
    private String topicOrderEvent;

    @Value("${app.kafka.topics.inventory-validated}")
    private String topicInventoryProcessed;

    @Value("${app.kafka.topics.payment-processed}")
    private String topicPaymentProcessed;

    private Consumer<String, String> testConsumer; // Consuming as raw String to avoid deserialization headaches

    @BeforeEach
    void setUpKafkaConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(),
                "test-catalog-group-integration",
                false // Uses the Testcontainers' container
        );

        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // due to async in test, let's manually commit

        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        testConsumer = cf.createConsumer();
        testConsumer.subscribe(Collections.singletonList(topicOrderEvent));
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    @Test
    @DisplayName("Should process complete successful Saga path changing order status to APPROVED asynchronously")
    void shouldExecuteCompleteSuccessfulSagaFlow() throws Exception {
        // --- ARRANGE: Prepare existing data in database ---
        String cartUuid = UUID.randomUUID().toString();
        Integer customerId = 82732917;
        Integer orderId = 4000;

        // Insert standard DRAFT order header
        jdbcTemplate.execute(String.format(
                "INSERT INTO orders (id, uuid, customer_id, status, inventory_status, payment_status, total_amount, price_updated_at) " +
                        "VALUES (%d, '%s', %d, 'DRAFT', 'DRAFT', 'DRAFT', 59.80, NOW())",
                orderId, cartUuid, customerId
        ));

        // Insert relational item to satisfy checkout internal rules
        jdbcTemplate.execute(String.format(
                "INSERT INTO order_items (order_id, product_id, quantity, price_at_purchase) " +
                        "VALUES (%d, 101, 1, 59.80)", orderId
        ));

        // Prepare HTTP Request Body with UUID inside JSON payload
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("uuid", cartUuid);
        requestBody.put("customerid", "1234");
        requestBody.put("paymentToken", "234343");
        requestBody.put("bearerToken", "fiejofw");

        // --- ACT PART 1: Trigger the Checkout via HTTP POST Body ---
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());

        // Validate synchronous step: Order must transition to PENDING first
        // Fetching the entire row as a Map to validate multiple columns in a single DB hit
        Map<String, Object> orderRow = jdbcTemplate.queryForMap(
                "SELECT status, inventory_status, payment_status FROM orders WHERE id = " + orderId);

        // Asserting the main Order state is now locked in PENDING
        assertEquals(OrderStatus.PENDING.name(), orderRow.get("status"));

        // Asserting Saga tracking columns are initialized (Adjust SagaStatus enum name if different, e.g., PENDING/PROCESSING)
        assertEquals(SagaStatus.PENDING.name(), orderRow.get("inventory_status"));
        assertEquals(SagaStatus.PENDING.name(), orderRow.get("payment_status"));

        // --- ACT PART 2: Simulate Inventory microservice success response ---
        InventoryValidatedEventDTO inventoryEvent = new InventoryValidatedEventDTO();
        inventoryEvent.setOrderId(orderId);
        inventoryEvent.setServiceStatus("SUCCESS");

        kafkaTemplate.send(topicInventoryProcessed, cartUuid, inventoryEvent);

        // --- ACT PART 3: Simulate Payment microservice success response ---
        PaymentValidatedEventDTO paymentEvent = new PaymentValidatedEventDTO();
        paymentEvent.setOrderId(orderId);
        paymentEvent.setServiceStatus("SUCCESS");

        kafkaTemplate.send(topicPaymentProcessed, cartUuid, paymentEvent);

        // --- ASSERT: Use Awaitility to poll database until asynchronous tasks finish ---
        await()
                .atMost(Duration.ofSeconds(5)) // Maximum time allowed for threads to finish
                .pollInterval(Duration.ofMillis(200)) // Checking database interval
                .untilAsserted(() -> {
                    String finalStatus = jdbcTemplate.queryForObject(
                            "SELECT status FROM orders WHERE id = " + orderId, String.class);

                    // The order should automatically become APPROVED when both workers return SUCCESS
                    assertEquals(OrderStatus.APPROVED.name(), finalStatus);
                });
    }

    @Test
    @DisplayName("Should process complete inventory failure compensation path and verify emitted Kafka event")
    void shouldCancelOrderAndVerifyEmittedKafkaEventOnInventoryFailure() throws Exception {
        // --- ARRANGE 1: Setup isolated data ---
        String cartUuid = UUID.randomUUID().toString();
        Integer customerId = 11111;
        Integer orderId = 5000;

        jdbcTemplate.execute(String.format(
                "INSERT INTO orders (id, uuid, customer_id, status, inventory_status, payment_status, total_amount, price_updated_at) " +
                        "VALUES (%d, '%s', %d, 'DRAFT', 'DRAFT', 'DRAFT', '100.00', NOW())",
                orderId, cartUuid, customerId
        ));

        jdbcTemplate.execute(String.format(
                "INSERT INTO order_items (order_id, product_id, quantity, price_at_purchase) " +
                        "VALUES (%d, 101, 2, '50.00')", orderId
        ));

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("uuid", cartUuid);

        // --- ACT 1: Trigger Checkout ---
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());

        // Check triggered event
        ConsumerRecord<String, String> responseRecord1 =
                KafkaTestUtils.getSingleRecord(testConsumer, topicOrderEvent, Duration.ofSeconds(15));

        assertThat(responseRecord1).isNotNull();
        assertThat(responseRecord1.key()).isEqualTo(cartUuid);
        assertThat(responseRecord1.value()).contains(OrderEventType.ORDER_PLACED.name());
        testConsumer.commitSync();

        // --- ACT 2: Microservices respond (Payment SUCCESS, Catalog FAILED) ---
        InventoryValidatedEventDTO inventoryEvent = new InventoryValidatedEventDTO();
        inventoryEvent.setOrderId(orderId);
        inventoryEvent.setServiceStatus("FAILED");
        kafkaTemplate.send(topicInventoryProcessed, cartUuid, inventoryEvent);

        PaymentValidatedEventDTO paymentSuccessEvent = new PaymentValidatedEventDTO();
        paymentSuccessEvent.setOrderId(orderId);
        paymentSuccessEvent.setServiceStatus("SUCCESS");
        kafkaTemplate.send(topicPaymentProcessed, cartUuid, paymentSuccessEvent);

        // --- ASSERT 1: Order must change to CANCELLED ---
        await()
                .atMost(Duration.ofSeconds(4))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    Map<String, Object> orderRow = jdbcTemplate.queryForMap(
                            "SELECT status, inventory_status FROM orders WHERE id = " + orderId);
                    assertEquals(OrderStatus.CANCELLED.name(), orderRow.get("status"));
                    assertEquals("FAILED", orderRow.get("inventory_status"));
                });

        // --- ASSERT 2: VERIFY EMITTED KAFKA EVENT
        // Captures the outbound message sent by order-service to notify payment about the failure
        ConsumerRecord<String, String> responseRecord2 =
                KafkaTestUtils.getSingleRecord(testConsumer, topicOrderEvent, Duration.ofSeconds(15));

        assertThat(responseRecord2).isNotNull();
        assertThat(responseRecord2.key()).isEqualTo(cartUuid);
        assertThat(responseRecord2.value()).contains(OrderEventType.STOCK_DECLINED.name());
        testConsumer.commitSync();

        // --- ACT 3: Simulate Payment reacting to STOCK_DECLINED and replying REFUNDED ---
        PaymentValidatedEventDTO paymentRefundEvent = new PaymentValidatedEventDTO();
        paymentRefundEvent.setOrderId(orderId);
        paymentRefundEvent.setServiceStatus("REFUNDED");
        kafkaTemplate.send(topicPaymentProcessed, cartUuid, paymentRefundEvent);

        // --- ASSERT 3: Final state validation showing full choreography closure ---
        await()
                .atMost(Duration.ofSeconds(4))
                .untilAsserted(() -> {
                    String currentPaymentStatus = jdbcTemplate.queryForObject(
                            "SELECT payment_status FROM orders WHERE id = " + orderId, String.class);
                    assertEquals("REFUNDED", currentPaymentStatus);
                });
    }

    @Test
    @DisplayName("Should process complete payment failure compensation path following strict choreography order")
    void shouldCancelOrderWhenPaymentFailsWithFullChoreography() throws Exception {
        // --- ARRANGE: Setup isolated data ---
        String cartUuid = UUID.randomUUID().toString();
        Integer customerId = 22222;
        Integer orderId = 6000;

        jdbcTemplate.execute(String.format(
                "INSERT INTO orders (id, uuid, customer_id, status, inventory_status, payment_status, total_amount, price_updated_at) " +
                        "VALUES (%d, '%s', %d, 'DRAFT', 'DRAFT', 'DRAFT', '150.00', NOW())",
                orderId, cartUuid, customerId
        ));

        jdbcTemplate.execute(String.format(
                "INSERT INTO order_items (order_id, product_id, quantity, price_at_purchase) " +
                        "VALUES (%d, 102, 3, '50.00')", orderId
        ));

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("uuid", cartUuid);

        // --- ACT 1: Trigger Checkout (Order goes to PENDING) ---
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());

        // Check triggered event
        ConsumerRecord<String, String> responseRecord1 =
                KafkaTestUtils.getSingleRecord(testConsumer, topicOrderEvent, Duration.ofSeconds(15));

        assertThat(responseRecord1).isNotNull();
        assertThat(responseRecord1.key()).isEqualTo(cartUuid);
        assertThat(responseRecord1.value()).contains(OrderEventType.ORDER_PLACED.name());
        testConsumer.commitSync();

        // --- ACT 2: Microservices respond (Catalog SUCCESS, Payment FAILED) ---
        InventoryValidatedEventDTO inventorySuccessEvent = new InventoryValidatedEventDTO();
        inventorySuccessEvent.setOrderId(orderId);
        inventorySuccessEvent.setServiceStatus("SUCCESS");
        kafkaTemplate.send(topicInventoryProcessed, cartUuid, inventorySuccessEvent);

        PaymentValidatedEventDTO paymentFailedEvent = new PaymentValidatedEventDTO();
        paymentFailedEvent.setOrderId(orderId);
        paymentFailedEvent.setServiceStatus("FAILED");
        kafkaTemplate.send(topicPaymentProcessed, cartUuid, paymentFailedEvent);

        // --- ASSERT 1: Order must detect failure, change to CANCELLED and update payment status ---
        await()
                .atMost(Duration.ofSeconds(4))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    Map<String, Object> orderRow = jdbcTemplate.queryForMap(
                            "SELECT status, payment_status FROM orders WHERE id = " + orderId);
                    assertEquals(OrderStatus.CANCELLED.name(), orderRow.get("status"));
                    assertEquals("FAILED", orderRow.get("payment_status"));
                });

        // --- ASSERT 2: VERIFY EMITTED KAFKA EVENT
        // Captures the outbound message sent by order-service to notify stock about the failure
        ConsumerRecord<String, String> responseRecord2 =
                KafkaTestUtils.getSingleRecord(testConsumer, topicOrderEvent, Duration.ofSeconds(15));

        assertThat(responseRecord2).isNotNull();
        assertThat(responseRecord2.key()).isEqualTo(cartUuid);
        assertThat(responseRecord2.value()).contains(OrderEventType.PAYMENT_DECLINED.name());
        testConsumer.commitSync();

        // --- ACT 3: Simulate Catalog microservice reacting to PAYMENT_DECLINED and replying RETURNED ---
        InventoryValidatedEventDTO inventoryReturnEvent = new InventoryValidatedEventDTO();
        inventoryReturnEvent.setOrderId(orderId);
        inventoryReturnEvent.setServiceStatus("RETURNED");
        kafkaTemplate.send(topicInventoryProcessed, cartUuid, inventoryReturnEvent);

        // --- ASSERT 2: Final state validation showing full choreography closure ---
        await()
                .atMost(Duration.ofSeconds(4))
                .untilAsserted(() -> {
                    String currentInventoryStatus = jdbcTemplate.queryForObject(
                            "SELECT inventory_status FROM orders WHERE id = " + orderId, String.class);
                    assertEquals("RETURNED", currentInventoryStatus);
                });
    }
}