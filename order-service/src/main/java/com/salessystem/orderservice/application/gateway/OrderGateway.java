package com.salessystem.orderservice.application.gateway;

import com.salessystem.orderservice.domain.Order;
import java.util.Optional;

public interface OrderGateway {
    public Optional<Order> findById(Integer id);

    Optional<Order> findByUuid(String uuid);

    Order save(Order order);
}
