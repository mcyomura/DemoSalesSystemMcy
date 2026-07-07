package com.salessystem.orderservice.application.usecase;

import com.salessystem.orderservice.application.exception.ResourceNotFoundException;
import com.salessystem.orderservice.application.gateway.OrderGateway;
import com.salessystem.orderservice.application.gateway.OrderMessageGateway;
import com.salessystem.orderservice.domain.Order;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConsultOrderUseCase {
    private final OrderGateway orderGateway;

    public ConsultOrderUseCase(OrderGateway orderGateway) {
        this.orderGateway = orderGateway;
    }

    public Order execute(Integer id) {
        Order order;

        // 1. Locate the cart
        Optional<Order> orderOpt = orderGateway.findById(id);

        if (orderOpt.isEmpty()){
            throw new ResourceNotFoundException("ID invalid:" + id);
        }
        order = orderOpt.get();

        return order;
    }
}
