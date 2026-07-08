package com.salessystem.orderservice.infra.persistence.mapper;

import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.domain.OrderItem;
import com.salessystem.orderservice.domain.OrderStatus;
import com.salessystem.orderservice.infra.persistence.entity.OrderDbEntity;
import com.salessystem.orderservice.infra.persistence.entity.OrderItemDbEntity;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")  // This makes this component be a bean (can be injected with @Autowired)
public interface OrderMapper {

    // 1. convertion DB to domain
    Order toDomainOrder (OrderDbEntity orderDb);

    @Mapping(target = "order", ignore = true)
    OrderItem toDomainOrderItem(OrderItemDbEntity orderItemDb);

    // 2. convertion domain to DB
    @Mapping(target = "createdAt", ignore = true)   //ignore field created_at, exists only for DB entity
    OrderDbEntity toEntityOrder(Order order);

    // Below: bidirectional relationship needs the following adjustment
    @Mapping(target = "order", ignore = true)   // order will enter as the OrderDbEntity.setItems
    OrderItemDbEntity toEntityOrderItem (OrderItem orderItem);


}
