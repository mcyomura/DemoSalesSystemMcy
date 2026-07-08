package com.salessystem.catalogservice.messaging;

import com.salessystem.catalogservice.dto.*;
import com.salessystem.catalogservice.service.InventoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OrderPlacedListener {
    private final InventoryService inventoryService;
    private static final Logger log = LoggerFactory.getLogger(OrderPlacedListener.class);

    @Value("${app.kafka.topics.inventory-validated}")
    private String inventoryValidatedTopic; // Reads the outbound topic name from properties
    private final KafkaTemplate<String, Object> kafkaTemplate; // Injected to send events back

    // Injecting the business service into the message consumer
    public OrderPlacedListener(InventoryService inventoryService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Listens to the queue and passes the verified DTO straight to the service layer
    @KafkaListener(topics = "${app.kafka.topics.order-event}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = "spring.json.value.default.type=com.salessystem.catalogservice.dto.OrderEventDTO")
    public void consume(OrderEventDTO event, @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        log.debug (" === Event received from order-event. Order ID: {}", event.getOrderId());

        switch (event.getOrderEventType()) {
            case OrderEventType.ORDER_PLACED:
                log.debug (" === Order placed - process stock update.");
                processOrderPlaced(event, key);
                break;
            case OrderEventType.PAYMENT_DECLINED:
                log.debug (" === Cancellation reason is PAYMENT_DECLINED. Return items to stock.");
                paymentDeclined(event, key);
                break;
            case OrderEventType.STOCK_DECLINED:
                log.debug (" === Cancellation reason is STOCK_DECLINED (already handled). No stock return needed.");
                break;
        }
    }

    private void processOrderPlaced(OrderEventDTO event, String key){
        try {
            // 1. Updates stock
            inventoryService.deductStock(event);

            // 2. Creates stock updated payload
            InventoryValidatedEventDTO response = new InventoryValidatedEventDTO(
                    event.getOrderId(),
                    StockResult.SUCCESS.name(), // Using Enum
                    "Stock successfully allocated."
            );

            // 3. Sends the stock updated event
            kafkaTemplate.send(inventoryValidatedTopic, key, response);
            log.debug("=== [Kafka Consumer] Event sent to kafka (stock update confirmation)");

        } catch (Exception e) {
            InventoryValidatedEventDTO response = new InventoryValidatedEventDTO(
                    event.getOrderId(),
                    StockResult.FAILED.name(), // Using Enum
                    e.getMessage());
            // Sends the stock updated failed event
            kafkaTemplate.send(inventoryValidatedTopic, key, response);

            // Logs the error safely without stopping the application
            log.error("!!! [Kafka Consumer Error] Failed to process stock update: " + e.getMessage());
        }
    }

    public void paymentDeclined(OrderEventDTO event, String key) {
        try {
            // Stock needs to be added back
            inventoryService.returnStock(event);

            InventoryValidatedEventDTO response = new InventoryValidatedEventDTO(
                    event.getOrderId(),
                    StockResult.RETURNED.name(), // Using Enum
                    "Items returned to stock."
            );

            // Sends the stock updated event
            kafkaTemplate.send(inventoryValidatedTopic, key, response);
            log.debug("=== [Kafka Consumer] Event stock returned sent to Kafka!");
        } catch (Exception e){
            log.error("Failed to return itens to stock. Order ID: {}. Trace:", event.getOrderId(), e);
        }
    }
}