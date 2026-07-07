package com.salessystem.orderservice.infra.web.mapper;

import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.domain.OrderItem;
import com.salessystem.orderservice.infra.web.dto.CartResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderWebMapper {

    // Traduz o nosso Domínio de volta para a Web (escondendo as datas automaticamente!)
    CartResponseDTO toResponse(Order domain);
    CartResponseDTO.OrderItemResponseDTO toResponseItem(OrderItem domainItem);
}