package com.salessystem.orderservice.application.gateway;

import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.domain.OrderEventType;

public interface OrderMessageGateway {
    // We pass the clean domain entity. Infra will handle conversion to DTO!
    void sendOrderPlacedEvent(Order order, String uuid, String paymentToken);
}