package com.salessystem.orderservice.infra.web.controller;

import com.salessystem.orderservice.application.usecase.ConsultOrderUseCase;
import com.salessystem.orderservice.application.usecase.ManageCartUseCase;
import com.salessystem.orderservice.application.usecase.OrderPlacedUseCase;
import com.salessystem.orderservice.domain.ManageCartResult;
import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.infra.web.dto.AddCartItemRequestDTO;
import com.salessystem.orderservice.infra.web.dto.CartResponseDTO;
import com.salessystem.orderservice.infra.web.dto.OrderPlacedRequestDTO;
import com.salessystem.orderservice.infra.web.dto.OrderStatusResponseDTO;
import com.salessystem.orderservice.infra.web.mapper.OrderWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${app.api.path.orders}")
public class OrderController {
    private final ManageCartUseCase manageCartUseCase;
    private final OrderPlacedUseCase orderPlacedUseCase;
    private final ConsultOrderUseCase consultOrder;
    private final OrderWebMapper orderWebMapper;

    public OrderController(ManageCartUseCase manageCartUseCase, OrderWebMapper orderWebMapper,
                           OrderPlacedUseCase orderPlacedUseCase, ConsultOrderUseCase consultOrder) {
        this.manageCartUseCase = manageCartUseCase;
        this.orderPlacedUseCase = orderPlacedUseCase;
        this.orderWebMapper = orderWebMapper;
        this.consultOrder = consultOrder;
    }

    @PostMapping ("/items")
    public ResponseEntity<CartResponseDTO> create(@RequestBody AddCartItemRequestDTO item) {

        // 1. executes the use case
        ManageCartResult cartResult = manageCartUseCase.executeAddItem(item.getUuid(), item.getCustomerId(),
                        item.getProductId(), item.getQuantity());

        // 2. converts domain to response
        Order createdOrderDomain = cartResult.order();
        CartResponseDTO response = orderWebMapper.toResponse(createdOrderDomain);

        // 3. Adds flag prices updated
        response.setPricesUpdated(cartResult.pricesUpdated());

        // 4. Returns 201 with the order containing ID and frozen price
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping ("/checkout")
    public ResponseEntity<OrderStatusResponseDTO> checkout(@RequestBody OrderPlacedRequestDTO orderPlacedRequestDTO){
        // 1. executes the use case
        Order orderUpdated = orderPlacedUseCase.execute(orderPlacedRequestDTO.getUuid(),
                orderPlacedRequestDTO.getCustomerId(), orderPlacedRequestDTO.getPaymentToken(),
                orderPlacedRequestDTO.getBearerToken());

        OrderStatusResponseDTO response = new OrderStatusResponseDTO (orderUpdated.getId(), orderUpdated.getCustomerId(),
                orderUpdated.getStatus().name(), orderUpdated.getInventoryStatus().name(),
                orderUpdated.getPaymentStatus().name(), orderUpdated.getTotalAmount());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderStatusResponseDTO> consultOrder(@PathVariable Integer id){
        // 1. executes the use case
        Order order = consultOrder.execute(id);

        OrderStatusResponseDTO response = new OrderStatusResponseDTO (order.getId(), order.getCustomerId(),
                order.getStatus().name(), order.getInventoryStatus().name(), order.getPaymentStatus().name(),
                order.getTotalAmount());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
