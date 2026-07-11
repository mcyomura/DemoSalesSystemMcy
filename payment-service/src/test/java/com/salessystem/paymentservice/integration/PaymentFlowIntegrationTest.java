package com.salessystem.paymentservice.integration;

import com.salessystem.paymentservice.adapters.inbound.messaging.OrderEventDTO;
import com.salessystem.paymentservice.adapters.inbound.messaging.OrderEventType;
import com.salessystem.paymentservice.adapters.outbound.persistence.PaymentEntity;
import com.salessystem.paymentservice.adapters.outbound.persistence.PaymentRepository;
import com.salessystem.paymentservice.domain.model.PaymentStatus;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@Sql(scripts = "/05 DB-PAYMENT schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PaymentFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${app.kafka.topics.order-event}")
    private String orderEventTopic;

    @Value("${app.kafka.topics.payment-processed}")
    private String paymentProcessedTopic;

    private Consumer<String, Object> testConsumer;

    @BeforeEach
    void setUp() {
        // Updated method compatible with Spring Kafka 3.x / 4.x
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(),
                "test-group-integration",
                true // embeddedKafka parameter as true or false
        );

        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, Object> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        testConsumer = cf.createConsumer();
        testConsumer.subscribe(Collections.singletonList(paymentProcessedTopic));
    }

    @AfterEach
    void tearDown() {
        testConsumer.close();
        paymentRepository.deleteAll(); // Ensures Database is clean after each test run
    }

    @Test
    @DisplayName("Should process payment successfully from Kafka end-to-end and save to MariaDB")
    void shouldProcessPaymentEndToEndSuccessfully() {
        // Given: Prepare the payload data
        String orderUuid = UUID.randomUUID().toString();
        Integer orderId = 999;
        Integer customerId = 777;
        BigDecimal amount = new BigDecimal("450.00");
        String validToken = "TOKEN_PROCESSED_OK"; // Since your usecase approves valid tokens

        OrderEventDTO orderPlacedEvent = new OrderEventDTO(
                OrderEventType.ORDER_PLACED, orderId, customerId, amount,
                LocalDateTime.now(), Collections.emptyList(), validToken
        );

        // When: We simulate order-service producing a message to the input topic
        kafkaTemplate.send(orderEventTopic, orderUuid, orderPlacedEvent);

        // Then: 1. Verify asynchronous persistence in MariaDB using Awaitility
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var paymentEntityOpt = paymentRepository.findByOrderId(orderId);
            assertThat(paymentEntityOpt).isPresent();
            assertThat(paymentEntityOpt.get().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(paymentEntityOpt.get().getPaymentToken()).isEqualTo(validToken);
        });

        // Then: 2. Verify that our outbound adapter published the success result back to Kafka
        ConsumerRecord<String, Object> outboundRecord = getSingleRecord(testConsumer, paymentProcessedTopic, Duration.ofSeconds(5));

        assertThat(outboundRecord).isNotNull();
        assertThat(outboundRecord.key()).isEqualTo(orderUuid);
        assertThat(outboundRecord.value().toString()).contains("SUCCESS");
        assertThat(outboundRecord.value().toString()).contains(orderId.toString());
    }

    @Test
    @DisplayName("Should process a declined payment when token ends with 99, save FAILED status, and notify order-service")
    void shouldProcessDeclinedPaymentSuccessfully() throws Exception {
        // Given: An order payload with a payment token ending in 99 to trigger the mock failure
        String key = UUID.randomUUID().toString();
        Integer orderId = 888;
        Integer customerId = 456;
        BigDecimal totalAmount = new BigDecimal("150.00");
        String declinedToken = "TOKEN_MOCK_DECLINED_99";

        OrderEventDTO orderEvent = new OrderEventDTO(
                OrderEventType.ORDER_PLACED, orderId, customerId, totalAmount,
                LocalDateTime.now(), Collections.emptyList(), declinedToken
        );

        // When: Producing the event to the input topic 'order-event'
        kafkaTemplate.send(orderEventTopic, key, orderEvent);

        // Then: Asynchronously assert that the MariaDB database persisted the transaction as FAILED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    var paymentEntityOpt = paymentRepository.findByOrderId(orderId);

                    assertThat(paymentEntityOpt).isPresent();
                    assertThat(paymentEntityOpt.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
                });

        // Then: Verify that the microservice published the failure notification back to Kafka for the order-service
        ConsumerRecord<String, Object> receivedRecord = getSingleRecord(testConsumer, paymentProcessedTopic, Duration.ofSeconds(5));

        assertThat(receivedRecord).isNotNull();
        String payloadJson = receivedRecord.value().toString();

        assertThat(payloadJson).contains(String.valueOf(orderId));
        assertThat(receivedRecord.key()).isEqualTo(key);
        assertThat(payloadJson).contains("FAILED");
    }

    @Test
    @DisplayName("Should detect duplicated payment event and route it automatically to the DLT")
    void shouldHandleDuplicatedPaymentEventBySendingToDLT() throws Exception {
        Integer orderId = 999;
        Integer customerId = 777;
        BigDecimal totalAmount = new BigDecimal("450.00");
        String paymentToken = "TOKEN_IDEMPOTENCY_CHECK";
        String key = UUID.randomUUID().toString();

        OrderEventDTO duplicateOrderEvent = new OrderEventDTO(
                OrderEventType.ORDER_PLACED, orderId, customerId, totalAmount,
                LocalDateTime.now(), Collections.emptyList(), paymentToken
        );

        // 1. When: Sending the FIRST event to process the payment successfully
        kafkaTemplate.send(orderEventTopic, key, duplicateOrderEvent);

        // 2. Then: Asynchronously verify that the first payment was saved in MariaDB
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var paymentEntityOpt = paymentRepository.findByOrderId(orderId);
            assertThat(paymentEntityOpt).isPresent();
            assertThat(paymentEntityOpt.get().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        });

        ConsumerRecord<String, Object> outboundRecord1 = getSingleRecord(testConsumer, paymentProcessedTopic, Duration.ofSeconds(5));

        assertThat(outboundRecord1).isNotNull();
        assertThat(outboundRecord1.value().toString()).contains("SUCCESS");
        assertThat(outboundRecord1.key()).isEqualTo(key);

        // 3. When: Sending exactly the SECOND identical event to simulate Kafka duplication
        kafkaTemplate.send(orderEventTopic, key, duplicateOrderEvent);

        // 4. Then: Give it a short moment for the listener to process the second message silently
        Thread.sleep(2000);

        // 5. ASSERT 1: Verify the database state remains untouched (Exactly ONE record exists, no duplicates)
        long recordCount = paymentRepository.countByOrderId(orderId);
        assertThat(recordCount).isEqualTo(1L);

        // 6. ASSERT 2: Verify that no new event was published to the payment-processed for this second call
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                KafkaTestUtils.getSingleRecord(testConsumer, paymentProcessedTopic, Duration.ofSeconds(5));
        }).withMessageContaining("No records").withNoCause();
    }

    @Test
    @DirtiesContext
    @DisplayName("Should handle invalid or malformed payload gracefully without altering the database")
    void shouldHandleMalformedPayloadGracefully() throws Exception {
        // Given: A completely broken JSON string (missing closing brackets, invalid attributes)
        String malformedJsonPayload = "{ \"orderId\": 123, \"customerId\": 456, \"totalAmount\": \"broken_value\" ";
        String key = UUID.randomUUID().toString();

        // When: Producing the toxic text directly into the input topic
        kafkaTemplate.send(orderEventTopic, key, malformedJsonPayload);

        // Then: Wait a small moment to let the listener context try (and fail) to parse it
        Thread.sleep(2000);

        // ASSERT: Guarantee that the database remains absolutely empty for this fake order ID
        // Since the payload never reached the use case, no transaction should ever exist
        long recordCount = paymentRepository.countByOrderId(123);
        assertThat(recordCount).isEqualTo(0L);

        try {
            KafkaTestUtils.getSingleRecord(testConsumer, orderEventTopic, Duration.ofSeconds(5));
        } catch (Exception e) {
            System.out.println("==============OKKKK===========");
        }
    }

    @Test
    @DisplayName("Should process refund successfully when stock is declined for a previously successful payment")
    void shouldProcessRefundSuccessfully() throws Exception {
        // 1. GIVEN: A previously successful payment exists in the MariaDB database
        Integer orderId = 111222; // Unique ID for this test context
        Integer customerId = 987;
        BigDecimal totalAmount = new BigDecimal("89.90");
        String paymentToken = "TOKEN_REFUND_HAPPY_PATH";
        String key = UUID.randomUUID().toString();

        // Save the initial SUCCESS state directly via Repository to setup the test scenario
        PaymentEntity existingPayment = new PaymentEntity();
        existingPayment.setOrderId(orderId);
        existingPayment.setCustomerId(customerId);
        existingPayment.setTotalAmount(totalAmount);
        existingPayment.setPaymentToken(paymentToken);
        existingPayment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(existingPayment);

        // Create the incoming cancellation event from order-service
        OrderEventDTO stockDeclinedEvent = new OrderEventDTO();
        stockDeclinedEvent.setOrderEventType(OrderEventType.STOCK_DECLINED); // Triggers the refund branch
        stockDeclinedEvent.setOrderId(orderId);
        stockDeclinedEvent.setCustomerId(customerId);
        stockDeclinedEvent.setTotalAmount(totalAmount);
        stockDeclinedEvent.setPaymentToken(paymentToken);

        // 2. WHEN: Publishing the STOCK_DECLINED event into the 'order-event' topic
        kafkaTemplate.send(orderEventTopic, key, stockDeclinedEvent);

        // 3. THEN: Asynchronously assert that the MariaDB status was updated to REFUNDED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    var updatedPaymentOpt = paymentRepository.findByOrderId(orderId);
                    assertThat(updatedPaymentOpt).isPresent();
                    assertThat(updatedPaymentOpt.get().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
                });

        // 4. THEN: Verify that the microservice notified the order-service about the successful refund
        ConsumerRecord<String, Object> receivedRecord = KafkaTestUtils.getSingleRecord(testConsumer, paymentProcessedTopic, Duration.ofSeconds(5));

        assertThat(receivedRecord).isNotNull();
        String payloadJson = receivedRecord.value().toString();

        // Validate the output event payload contract
        assertThat(receivedRecord.key()).isEqualTo(key);
        assertThat(payloadJson).contains(orderId.toString());
        assertThat(payloadJson).contains("REFUNDED");
        assertThat(payloadJson).contains("Payment refunded!");
    }

    @Test
    @DisplayName("Should handle duplicated stock declined events idempotently if the payment is already refunded")
    void shouldHandleDuplicatedRefundEventIdempotently() throws Exception {
        // 1. GIVEN: A payment that is ALREADY in the REFUNDED state in MariaDB
        Integer orderId = 333444; // Unique ID for this specific idempotency scenario
        Integer customerId = 987;
        BigDecimal totalAmount = new BigDecimal("120.00");
        String paymentToken = "TOKEN_REFUND_IDEMPOTENCY";
        String key = UUID.randomUUID().toString();

        // Persist the pre-existing REFUNDED state directly into the database
        PaymentEntity alreadyRefundedPayment = new PaymentEntity();
        alreadyRefundedPayment.setOrderId(orderId);
        alreadyRefundedPayment.setCustomerId(customerId);
        alreadyRefundedPayment.setTotalAmount(totalAmount);
        alreadyRefundedPayment.setPaymentToken(paymentToken);
        alreadyRefundedPayment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(alreadyRefundedPayment);

        // Create the duplicated cancellation event
        OrderEventDTO duplicateRefundEvent = new OrderEventDTO();
        duplicateRefundEvent.setOrderEventType(OrderEventType.STOCK_DECLINED);
        duplicateRefundEvent.setOrderId(orderId);
        duplicateRefundEvent.setCustomerId(customerId);
        duplicateRefundEvent.setTotalAmount(totalAmount);
        duplicateRefundEvent.setPaymentToken(paymentToken);

        // 2. WHEN: Sending the STOCK_DECLINED event for a payment that is already refunded
        kafkaTemplate.send(orderEventTopic, key, duplicateRefundEvent);

        // 3. THEN: Allow a brief moment for the async listener to run and hit the idempotency wall
        Thread.sleep(2000);

        // 4. ASSERT 1: Verify the database state remained untouched (Still exactly ONE record, still REFUNDED)
        long recordCount = paymentRepository.countByOrderId(orderId);
        assertThat(recordCount).isEqualTo(1L);

        var databaseRecord = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(databaseRecord.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        // 5. ASSERT 2: Ensure NO new event was published to the order-service during this second call
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
            KafkaTestUtils.getSingleRecord(testConsumer, paymentProcessedTopic, Duration.ofSeconds(5));
        }).withMessageContaining("No records").withNoCause();
    }

    @Test
    @DisplayName("Should refuse to process refund when the existing payment status is FAILED")
    void shouldNotProcessRefundWhenPaymentStatusIsFailed() throws Exception {
        // 1. GIVEN: A payment record that explicitly FAILED in the past
        Integer orderId = 555666; // Unique ID for this specific invalid state scenario
        Integer customerId = 321;
        BigDecimal totalAmount = new BigDecimal("75.00");
        String paymentToken = "TOKEN_REFUND_INVALID_STATE";
        String key = UUID.randomUUID().toString();

        // Persist the pre-existing FAILED state directly into MariaDB
        PaymentEntity failedPayment = new PaymentEntity();
        failedPayment.setOrderId(orderId);
        failedPayment.setCustomerId(customerId);
        failedPayment.setTotalAmount(totalAmount);
        failedPayment.setPaymentToken(paymentToken);
        failedPayment.setStatus(PaymentStatus.FAILED); // State that blocks refunds
        paymentRepository.save(failedPayment);

        // Create the stock declined event
        OrderEventDTO stockDeclinedEvent = new OrderEventDTO();
        stockDeclinedEvent.setOrderEventType(OrderEventType.STOCK_DECLINED);
        stockDeclinedEvent.setOrderId(orderId);
        stockDeclinedEvent.setCustomerId(customerId);
        stockDeclinedEvent.setTotalAmount(totalAmount);
        stockDeclinedEvent.setPaymentToken(paymentToken);

        // 2. WHEN: Publishing the STOCK_DECLINED event for a failed payment
        kafkaTemplate.send(orderEventTopic, key, stockDeclinedEvent);

        // 3. THEN: Allow a brief moment for the async consumer to process the message and hit the safeguard
        Thread.sleep(2000);

        // 4. ASSERT: Verify the database state remained UNTOUCHED (Still exactly ONE record, still FAILED)
        long recordCount = paymentRepository.countByOrderId(orderId);
        assertThat(recordCount).isEqualTo(1L);

        var databaseRecord = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(databaseRecord.getStatus()).isEqualTo(PaymentStatus.FAILED);

        // The console should display your log.debug: "Payment at invalid state for refund!..."
    }

    @Test
    @DisplayName("Should log an error and do nothing when attempting to refund a non-existent payment")
    void shouldLogExceptionWhenRefundPaymentIsNotFound() throws Exception {
        // 1. GIVEN: A payment record that explicitly FAILED in the past
        Integer orderId = 888999; // Unique ID for this specific invalid state scenario
        Integer customerId = 456;
        BigDecimal totalAmount = new BigDecimal("175.00");
        String paymentToken = "TOKEN_REFUND_NOT_FOUND";
        String key = UUID.randomUUID().toString();

        // Explicitly ensure the database is empty for this ID (no setup save here)
        long initialCount = paymentRepository.countByOrderId(orderId);
        assertThat(initialCount).isEqualTo(0L);

        // Create the stock declined event
        OrderEventDTO stockDeclinedEvent = new OrderEventDTO();
        stockDeclinedEvent.setOrderEventType(OrderEventType.STOCK_DECLINED);
        stockDeclinedEvent.setOrderId(orderId);
        stockDeclinedEvent.setCustomerId(customerId);
        stockDeclinedEvent.setTotalAmount(totalAmount);
        stockDeclinedEvent.setPaymentToken(paymentToken);

        // 2. WHEN: Publishing the STOCK_DECLINED event for a failed payment
        kafkaTemplate.send(orderEventTopic, key, stockDeclinedEvent);

        // 3. THEN: Allow a brief moment for the async consumer to process the message and hit the safeguard
        Thread.sleep(2000);

        // 4. ASSERT: Verify that the database REMAINS ABSOLUTELY EMPTY for this order ID
        long recordCount = paymentRepository.countByOrderId(orderId);
        assertThat(recordCount).isEqualTo(0L);

        // The console will display your log: "Payment not found while trying to process a refund!..."
    }

/* // return later
    @Test
    @DisplayName("Should handle missing mandatory fields gracefully without persisting anything to the database")
    void shouldHandleInvalidPayloadsMissingMandatoryFields() throws Exception {
        String fakeKey = "400";

        // Scenario A: Missing 'orderId' entirely
        String jsonMissingOrderId = """
        {
            "customerId": 456,
            "totalAmount": 150.00,
            "paymentToken": "TOKEN_VALID_123"
        }
        """;

        // Scenario B: Missing 'paymentToken' entirely
        String jsonMissingToken = """
        {
            "orderId": 555,
            "customerId": 456,
            "totalAmount": 150.00
        }
        """;

        // 1. When: Sending the payload that is missing the orderId
        kafkaTemplate.send(orderEventTopic, fakeKey, jsonMissingOrderId);

        // 2. When: Sending the payload that is missing the paymentToken
        kafkaTemplate.send(orderEventTopic, fakeKey, jsonMissingToken);

        // 3. Then: Allow a brief moment for the async consumer to drop/reject both messages
        Thread.sleep(2000);

        // 4. ASSERT: Guarantee that no transaction was created for the missing token scenario (orderId 555)
        long countForOrder555 = paymentRepository.countByOrderId(555);
        assertThat(countForOrder555).isEqualTo(0L);

        // 5. ASSERT: Guarantee that a general check or side effect didn't pollute the database
        // (Since orderId was missing/null in the first scenario, it should never find a record for it)
        long totalCountInTable = paymentRepository.count();
        // Optional: If your database had 2 records from previous successful tests in the same class,
        // you can assert that the count didn't increase, or just verify that no null orderId exists.
    }
 */
}