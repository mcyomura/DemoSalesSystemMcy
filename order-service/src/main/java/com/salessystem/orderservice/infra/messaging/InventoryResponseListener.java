package com.salessystem.orderservice.infra.messaging;

import com.salessystem.orderservice.domain.*;
import com.salessystem.orderservice.infra.messaging.dto.InventoryValidatedEventDTO;
import com.salessystem.orderservice.infra.messaging.dto.OrderEventDTO;
import com.salessystem.orderservice.infra.messaging.mapper.OrderEventMapper;
import com.salessystem.orderservice.infra.persistence.OrderRepositoryGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class InventoryResponseListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentResponseListener.class);

    private final OrderRepositoryGateway orderRepositoryGtw;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderEventMapper orderEventMapper;

    @Value("${app.kafka.topics.order-event}")
    private String topicOrderEvent;

    // Manual constructor injection
    public InventoryResponseListener(OrderRepositoryGateway orderRepositoryGtw, KafkaTemplate<String, Object> kafkaTemplate,
                                     OrderEventMapper orderEventMapper) {
        this.orderRepositoryGtw = orderRepositoryGtw;
        this.kafkaTemplate = kafkaTemplate;
        this.orderEventMapper = orderEventMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.inventory-validated}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = "spring.json.value.default.type=com.salessystem.orderservice.infra.messaging.dto.InventoryValidatedEventDTO")
    @Transactional
    public void consume(InventoryValidatedEventDTO response) {
        log.debug("=== [Kafka Consumer] Received inventory validation for ID: " + response.getOrderId());

        // 1. Fetch the order from the database using the ID
        Order order = orderRepositoryGtw.findById(response.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found for ID: " + response.getOrderId()));

        // 2. Update the specific stock validation column with the int value (2 or -1)
        // Note: Change 'StockStatus' to the exact name of the setter method in your Order class
        order.setInventoryStatus(response.getServiceStatus());
        log.debug(" -> Stock validation status updated to: " + response.getServiceStatus());

        switch (response.getServiceStatus())
        {
            case ServiceConfirmationStatus.SUCCESS:
                // 3. If stock is SUCCESS (2) and payment is SUCCESS (2), order becomes CONFIRMED
                if (order.getInventoryStatus() == ServiceConfirmationStatus.SUCCESS &&
                        order.getPaymentStatus() == ServiceConfirmationStatus.SUCCESS) {
                    order.setStatus(OrderStatus.APPROVED);
                    log.debug(" ===  Order set to CONFIRMED!");
                }

                orderRepositoryGtw.save(order);
                break;

            case ServiceConfirmationStatus.FAILED:
                // If the catalog rejected the items, the whole order fails
                order.setStatus(OrderStatus.CANCELLED);
                // Save the updated order state back to the database
                orderRepositoryGtw.save(order);

                // Create the compensation event
                OrderEventDTO orderEventDto = orderEventMapper.toEventDto(order);
                orderEventDto.setOrderEventType(OrderEventType.STOCK_DECLINED);

                // 3. Publish to the order-event topic using UUID as key (will for sure process AFTER the order placed event)
                kafkaTemplate.send(topicOrderEvent, order.getUuid(), orderEventDto);

                log.debug(" !!! Order CANCELED due to lack of stock. Reason: " + response.getReason());
                log.debug(" === [Saga Orchestrator] Compensation event sent to topic: order-canceled ===");
                break;
            case ServiceConfirmationStatus.ROLLBACK:
                // Save the updated inventory status to the database
                orderRepositoryGtw.save(order);
                break;
            default:
                log.error("Invalid status from Inventory service");
        }
    }
}