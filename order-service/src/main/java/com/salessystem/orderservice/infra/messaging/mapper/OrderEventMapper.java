package com.salessystem.orderservice.infra.messaging.mapper;

import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.domain.OrderItem;
import com.salessystem.orderservice.infra.messaging.dto.OrderEventDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring") // Allows Spring to inject this mapper automatically as a Bean
public interface OrderEventMapper {

    // Main mapping method: Domain Order -> Integration Event DTO
    @Mapping(source = "id", target = "orderId")
    OrderEventDTO toEventDto(Order order);

    // Child mapping method: MapStruct uses this implicitly to convert the items list
    OrderEventDTO.OrderItemEventDTO toOrderItemEventDto(OrderItem item);
}