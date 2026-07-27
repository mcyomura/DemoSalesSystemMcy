package com.salessystem.orderservice.application.usecase;

import com.salessystem.orderservice.application.exception.IllegalOrderStateException;
import com.salessystem.orderservice.application.exception.ResourceNotFoundException;
import com.salessystem.orderservice.application.gateway.OrderGateway;
import com.salessystem.orderservice.application.gateway.OrderMessageGateway;
import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.domain.OrderStatus;
import com.salessystem.orderservice.domain.SagaStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrderPlacedUseCase {
    private final OrderGateway orderGateway;
    private final OrderMessageGateway orderMessageGateway;

    public OrderPlacedUseCase(OrderGateway orderGateway, OrderMessageGateway orderMessageGateway) {
        this.orderGateway = orderGateway;
        this.orderMessageGateway = orderMessageGateway;
    }

    public Order execute(String uuid, Integer customerId, String payToken) {
        Order order;

        // 1. Locate the cart
        Optional<Order> orderOpt = orderGateway.findByUuid(uuid);
        if (orderOpt.isEmpty()){
            throw new ResourceNotFoundException("UUID invalid:" + uuid);
        }
        order = orderOpt.get();

        if (order.getStatus() != OrderStatus.DRAFT){
            throw new IllegalOrderStateException("Shopping cart already closed. UUID:" + uuid);
        }

        // 2. Change status of cart, plus stock and payment confirmation statuses to PENDING
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);
        order.setInventoryStatus(SagaStatus.PENDING);
        order.setPaymentStatus(SagaStatus.PENDING);

        // 3. Save the order with statuses updated
        Order savedOrder =orderGateway.save(order);

        // 4. ASYNC NOTIFICATION: Trigger the message to Kafka
        orderMessageGateway.sendOrderPlacedEvent(order, uuid, payToken);

        return savedOrder;
    }

}
