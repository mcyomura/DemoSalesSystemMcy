package com.salessystem.orderservice.infra.messaging;

import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.application.gateway.OrderMessageGateway;
import com.salessystem.orderservice.domain.OrderEventType;
import com.salessystem.orderservice.infra.messaging.dto.OrderEventDTO;
import com.salessystem.orderservice.infra.messaging.mapper.OrderEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer implements OrderMessageGateway { // Implementing the Domain Port

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderEventMapper orderEventMapper;

    @Value("${app.kafka.topics.order-event}")
    private String topicName;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate, OrderEventMapper orderEventMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderEventMapper = orderEventMapper;
    }

    @Override
    public void sendOrderPlacedEvent(Order order, String uuid, String payToken) {
        OrderEventDTO eventDto = orderEventMapper.toEventDto(order);
        eventDto.setOrderEventType(OrderEventType.ORDER_PLACED);
        eventDto.setPaymentToken(payToken);

        log.info("Publishing order event to Kafka topic '{}'...", topicName);
        this.kafkaTemplate.send(topicName, uuid, eventDto);
        log.info("Event successfully sent to Kafka broker!");
    }
}