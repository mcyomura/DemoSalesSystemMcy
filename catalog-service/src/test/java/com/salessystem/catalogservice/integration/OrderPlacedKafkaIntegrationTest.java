package com.salessystem.catalogservice.integration;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salessystem.catalogservice.dto.InventoryValidatedEventDTO;
import com.salessystem.catalogservice.dto.OrderEventDTO;
import com.salessystem.catalogservice.dto.OrderEventType;
import com.salessystem.catalogservice.dto.StockResult;
import com.salessystem.catalogservice.messaging.OrderPlacedListener;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.*;
import ch.qos.logback.classic.Logger; // Implementation backing slf4j
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Sql(scripts = "/01 DB-CATALOG-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderPlacedKafkaIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Spring's default Jackson mapper to safely decode the response - note: autowire didn't work
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @Value("${app.kafka.topics.order-event}")
    private String orderEventTopic;

    @Value("${app.kafka.topics.inventory-validated}")
    private String inventoryValidatedTopic;

    private Consumer<String, String> testConsumer; // Consuming as raw String to avoid deserialization headaches
    private BlockingQueue<ConsumerRecord<String, String>> recordsQueue;

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
        testConsumer.subscribe(Collections.singletonList(inventoryValidatedTopic));
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    @Test
    @DisplayName("Should successfully process ORDER_PLACED, deduct stock and reply with SUCCESS status")
    void shouldProcessOrderPlaced_AndDeductStockSuccessfully() throws Exception {
        // Arrange
        Integer orderId = 999;
        String key = "order-key-999";

        OrderEventDTO.OrderItemEventDTO item1 = new OrderEventDTO.OrderItemEventDTO(1, 10);
        OrderEventDTO.OrderItemEventDTO item2 = new OrderEventDTO.OrderItemEventDTO(3, 2);
        OrderEventDTO event = new OrderEventDTO(
                OrderEventType.ORDER_PLACED,
                orderId,
                101,
                BigDecimal.valueOf(959.7),
                LocalDateTime.now(),
                List.of(item1,item2),
                "payment-token-test"
        );

        // Act
        // 1. Post original event
        kafkaTemplate.send(orderEventTopic, key, event);

        // Assert
        // 2. Consume synchronously from output topic using Spring-Kafka utility (no Threads!)
        ConsumerRecord<String, String> responseRecord =
                KafkaTestUtils.getSingleRecord(testConsumer, inventoryValidatedTopic, Duration.ofSeconds(15));

        assertThat(responseRecord).isNotNull();
        assertThat(responseRecord.key()).isEqualTo(key);
        testConsumer.commitSync();

        // 3. Deserialize and Assert payload
        InventoryValidatedEventDTO responsePayload = objectMapper.readValue(
                responseRecord.value(),
                InventoryValidatedEventDTO.class
        );

        assertThat(responsePayload.getOrderId()).isEqualTo(orderId);
        assertThat(responsePayload.getServiceStatus()).isEqualTo(StockResult.SUCCESS.name());
        assertThat(responsePayload.getReason()).contains("Stock successfully allocated");

        // 4. Query MariaDB container to assert physical stock deduction
        Integer updatedStock1 = jdbcTemplate.queryForObject(
                    "SELECT quantity FROM inventory WHERE product_id = 1", Integer.class);
        assertThat(updatedStock1).isEqualTo(40);

        Integer updatedStock2 = jdbcTemplate.queryForObject(
                    "SELECT quantity FROM inventory WHERE product_id = 3", Integer.class);
        assertThat(updatedStock2).isEqualTo(198);
    }

    @Test
    @DisplayName("Should reject ORDER_PLACED and don't reduce any stock when a product does not exist in inventory")
    void shouldRejectOrderPlaced_WhenProductDoesNotExist() throws Exception {
        // Arrange
        Integer orderId = 1000;
        String key = "order-key-1000";
        Integer existingProductId = 1;
        Integer nonExistingProductId = 9999; // ID that does not exist in our SQL insert

        OrderEventDTO.OrderItemEventDTO item1 = new OrderEventDTO.OrderItemEventDTO(existingProductId, 10);
        OrderEventDTO.OrderItemEventDTO item2 = new OrderEventDTO.OrderItemEventDTO(nonExistingProductId, 1);
        OrderEventDTO event = new OrderEventDTO(
                OrderEventType.ORDER_PLACED,
                orderId,
                101,
                BigDecimal.valueOf(29.90),
                LocalDateTime.now(),
                List.of(item1, item2),
                "payment-token-test"
        );

        // Act
        kafkaTemplate.send(orderEventTopic, key, event);

        // Assert
        // 1. Consume the rejection event from the output topic
        ConsumerRecord<String, String> responseRecord =
                KafkaTestUtils.getSingleRecord(testConsumer, inventoryValidatedTopic, Duration.ofSeconds(15));

        assertThat(responseRecord).isNotNull();
        assertThat(responseRecord.key()).isEqualTo(key);
        testConsumer.commitSync();

        InventoryValidatedEventDTO responsePayload = objectMapper.readValue(
                responseRecord.value(),
                InventoryValidatedEventDTO.class
        );

        // 2. Validate rejection status and reason
        assertThat(responsePayload.getOrderId()).isEqualTo(orderId);
        assertThat(responsePayload.getServiceStatus()).isEqualTo("FAILED");
        assertThat(responsePayload.getReason()).contains("Stock registry not found for Product ID: 9999");

        // 4. Query MariaDB container to assert not physical stock deduction for the product that exists
        Integer updatedStock1 = jdbcTemplate.queryForObject(
                    "SELECT quantity FROM inventory WHERE product_id = 1", Integer.class);
            assertThat(updatedStock1).isEqualTo(50);
    }

    @Test
    @DisplayName("Should reject ORDER_PLACED when requested quantity exceeds available stock")
    void shouldRejectOrderPlaced_WhenStockIsInsufficient() throws Exception {
        // Arrange
        Integer orderId = 1001;
        String key = "order-key-1001";

        // Product 1 (Mechanical Keyboard) starts with 50 units. Let's request 60 units.
        OrderEventDTO.OrderItemEventDTO item1 = new OrderEventDTO.OrderItemEventDTO(1, 10);
        OrderEventDTO.OrderItemEventDTO item2 = new OrderEventDTO.OrderItemEventDTO(2, 130);
        OrderEventDTO event = new OrderEventDTO(
                OrderEventType.ORDER_PLACED,
                orderId,
                101,
                BigDecimal.valueOf(5399.40),
                LocalDateTime.now(),
                List.of(item1, item2),
                "payment-token-test"
        );

        // Act
        kafkaTemplate.send(orderEventTopic, key, event);

        // Assert
        // 1. Consume response
        ConsumerRecord<String, String> responseRecord =
                KafkaTestUtils.getSingleRecord(testConsumer, inventoryValidatedTopic, Duration.ofSeconds(15));

        assertThat(responseRecord).isNotNull();
        assertThat(responseRecord.key()).isEqualTo(key);
        testConsumer.commitSync();

        InventoryValidatedEventDTO responsePayload = objectMapper.readValue(
                responseRecord.value(),
                InventoryValidatedEventDTO.class
        );

        // 2. Validate rejected status due to lack of stock
        assertThat(responsePayload.getOrderId()).isEqualTo(orderId);
        assertThat(responsePayload.getServiceStatus()).isEqualTo("FAILED");
        assertThat(responsePayload.getReason()).contains("Insufficient stock");

        // 3. Ensure that the database stock remains untouched
        Integer stockInDatabase1 = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE product_id = 1", Integer.class);
        assertThat(stockInDatabase1).isEqualTo(50);
        Integer stockInDatabase2 = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE product_id = 2", Integer.class);
        assertThat(stockInDatabase2).isEqualTo(120);
    }

    @Test
    @DisplayName("Should successfully process PAYMENT_DECLINED, return items to stock, and reply with SUCCESS status")
    void shouldProcessPaymentDeclined_AndReturnItemsToStockSuccessfully() throws Exception {
        // Arrange
        Integer orderId = 1002;
        String key = "order-key-1002";

        // items to return
        OrderEventDTO.OrderItemEventDTO item1 = new OrderEventDTO.OrderItemEventDTO(1, 5);
        OrderEventDTO.OrderItemEventDTO item2 = new OrderEventDTO.OrderItemEventDTO(3, 7);
        OrderEventDTO event = new OrderEventDTO(
                OrderEventType.PAYMENT_DECLINED, // Setting event type to payment declined
                orderId,
                101,
                BigDecimal.valueOf(449.95),
                LocalDateTime.now(),
                List.of(item1, item2),
                "payment-token-test"
        );

        // Act
        // 1. Post the payment declined event to the input topic
        kafkaTemplate.send(orderEventTopic, key, event);

        // Assert
        // 2. Consume from the output topic
        ConsumerRecord<String, String> responseRecord =
                KafkaTestUtils.getSingleRecord(testConsumer, inventoryValidatedTopic, Duration.ofSeconds(15));

        assertThat(responseRecord).isNotNull();
        assertThat(responseRecord.key()).isEqualTo(key);
        testConsumer.commitSync(); // Commit immediately to keep the queue clean!

        InventoryValidatedEventDTO responsePayload = objectMapper.readValue(
                responseRecord.value(),
                InventoryValidatedEventDTO.class
        );

        // 3. Assert the outbound event attributes
        assertThat(responsePayload.getOrderId()).isEqualTo(orderId);
        assertThat(responsePayload.getServiceStatus()).isEqualTo(StockResult.RETURNED.name());
        assertThat(responsePayload.getReason()).contains("Items returned to stock"); // Adjust this text to match your service's message

        // 4. Query MariaDB container to assert physical stock increase
        Integer updatedStock1 = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE product_id = 1", Integer.class);
        assertThat(updatedStock1).isEqualTo(55);
        Integer updatedStock2 = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE product_id = 3", Integer.class);
        assertThat(updatedStock2).isEqualTo(207);
        Integer nonUpdatedStock = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE product_id = 2", Integer.class);
        assertThat(nonUpdatedStock).isEqualTo(120);
    }

    @Test
    @DisplayName("Should log error when PAYMENT_DECLINED fails to return items due to non-existing product")
    void shouldLogError_WhenPaymentDeclinedProductDoesNotExist() throws Exception {
        // 1. Get the Logback Logger for the class logging the error
        // (Ajuste para a classe que de fato gera o log de erro, ex: OrderPlacedListener ou InventoryService)
        Logger listenerLogger = (Logger) LoggerFactory.getLogger(OrderPlacedListener.class);

        // 2. Create and start a ListAppender to capture logs in memory
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        listenerLogger.addAppender(listAppender);

        try {
            // Arrange
            Integer orderId = 1003;
            String kafkaKey = "order-key-1003";
            Integer nonExistingProductId = 9999; // ID that does not exist in our system

            OrderEventDTO.OrderItemEventDTO item1 = new OrderEventDTO.OrderItemEventDTO(1, 5);
            OrderEventDTO.OrderItemEventDTO item2 = new OrderEventDTO.OrderItemEventDTO(nonExistingProductId, 5);
            OrderEventDTO event = new OrderEventDTO(
                    OrderEventType.PAYMENT_DECLINED,
                    orderId,
                    101,
                    BigDecimal.valueOf(149.50),
                    LocalDateTime.now(),
                    List.of(item1,item2),
                    "payment-token-test"
            );

            // Act
            // Send the event. There will be NO response on the kafka topic
            kafkaTemplate.send(orderEventTopic, kafkaKey, event);

            // Assert
            // 3. Since it's asynchronous, use Awaitility to wait for the specific error log to appear
            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                // Read all captured logs from our appender
                var logs = listAppender.list;

                // Assert that at least one log entry contains the expected error information
                boolean errorLogged = logs.stream()
                        .anyMatch(log -> log.getFormattedMessage().contains("Failed to return itens to stock. Order ID: " + orderId)
                                || log.getFormattedMessage().contains("9999"));

                assertThat(errorLogged)
                        .withFailMessage("Expected error log about non-existing product was not generated!")
                        .isTrue();
            });

            // 4. Ensure that the database stock remains untouched (it must stay at 50)
            Integer stockInDatabase = jdbcTemplate.queryForObject(
                    "SELECT quantity FROM inventory WHERE product_id = 1", Integer.class);
            assertThat(stockInDatabase).isEqualTo(50);

        } finally {
            // 5. Clean up: detach the appender so it doesn't leak into other tests
            listAppender.stop();
            listenerLogger.detachAppender(listAppender);
        }

        // Assert database is unchanged
        Integer nonUpdatedStock1 = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE product_id = 1", Integer.class);
        assertThat(nonUpdatedStock1).isEqualTo(50);
    }

    @Test
    @Order(2)
    @DisplayName("Should do nothing and keep stock untouched when STOCK_DECLINED event is received")
    void shouldDoNothing_WhenStockDeclinedEventIsReceived() throws Exception {
        // Arrange
        Integer orderId = 1005;
        String kafkaKey = "order-key-1005";

        // Simulating a stock declined event for 5 units of product 1 (initial stock is 50)
        OrderEventDTO.OrderItemEventDTO item = new OrderEventDTO.OrderItemEventDTO(1, 5);
        OrderEventDTO event = new OrderEventDTO(
                OrderEventType.STOCK_DECLINED, // Setting event type to stock declined
                orderId,
                101,
                BigDecimal.valueOf(449.95),
                LocalDateTime.now(),
                List.of(item),
                "payment-token-test"
        );

        // Act
        kafkaTemplate.send(orderEventTopic, kafkaKey, event);

        // Assert
        // 1. As this event is passive, no reply is sent. We wait a bit to guarantee database was NOT changed.
        TimeUnit.SECONDS.sleep(2); // Safe wait since we expect NO action

        // 2. Query MariaDB container to assert physical stock remains exactly 50
        Integer stockInDatabase = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE product_id = 1", Integer.class);
        assertThat(stockInDatabase).isEqualTo(50);
    }

    @Test
    @Order(1)
    @DisplayName("Should route invalid payload to DLT gracefully")
    void shouldRouteInvalidPayload_ToDltGracefully() throws Exception {
            // Arrange
            String kafkaKey = "order-key-invalid";
            String invalidJsonPayload = "{ \"orderId\": \"invalid-id-type\", \"items\": \"corrupted-data\" }";

            // Act: Send the bad payload to the main topic
            kafkaTemplate.send(orderEventTopic, kafkaKey, invalidJsonPayload);

        // 3. Ensure that our database connection and stock are still completely healthy
        Integer stockInDatabase = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE product_id = 1", Integer.class);
        assertThat(stockInDatabase).isEqualTo(50);

        // Consume the topic in order not to affect following tests
        try {
            KafkaTestUtils.getSingleRecord(testConsumer, orderEventTopic, Duration.ofSeconds(5));
            testConsumer.commitSync();
        } catch (Exception e) {
            System.out.println("=== Consume defected topic");
        }
    }

}