package com.salessystem.bff.controller;

import com.salessystem.bff.dto.cart.AddItemRequestDTO;
import com.salessystem.bff.dto.cart.CartResponseDTO;
import com.salessystem.bff.dto.cart.CheckoutRequestDTO;
import com.salessystem.bff.client.OrderClient;
import com.salessystem.bff.dto.cart.OrderStatusResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${app.api.path.bff}")
public class CartBffController {

    private static final Logger log = LoggerFactory.getLogger(CartBffController.class);
    private final OrderClient orderClient;

    public CartBffController(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @PostMapping("/cart/items")
    public ResponseEntity<CartResponseDTO> items(@RequestBody AddItemRequestDTO request) {
        log.info("=== [BFF Gateway] Add item request received from Frontend. Customer ID: {}", request.getCustomerId());

        // Routing the call transparently to order-service via OpenFeign
        CartResponseDTO cartResponseDTO = orderClient.addItemToCart(request);

        log.info(" -> Success: Add item triggered successfully via Feign.");
        return ResponseEntity.status(HttpStatus.CREATED).body(cartResponseDTO);
    }

    @PostMapping("/cart/checkout")
    public ResponseEntity<OrderStatusResponseDTO> checkout(@RequestBody CheckoutRequestDTO request) {
        log.info("=== [BFF Gateway] Checkout request received from Frontend. Customer ID: {}", request.getCustomerId());

        // Routing the call transparently to order-service via OpenFeign
        OrderStatusResponseDTO orderStatusResponseDTO =  orderClient.confirmCheckout(request);

        log.info(" -> Success: Order creation triggered successfully via Feign.");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(orderStatusResponseDTO);
    }

    @GetMapping("/cart/{id}")
    public ResponseEntity<OrderStatusResponseDTO> consultOrder(@PathVariable Integer id){
        log.info("=== [BFF Gateway] Consult order request received from Frontend. Order ID: {}", id);

        // Routing the call transparently to order-service via OpenFeign
        OrderStatusResponseDTO orderStatusResponseDTO =  orderClient.consultOrder(id);

        log.info(" -> Success: Consult triggered successfully via Feign.");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(orderStatusResponseDTO);
     }
}