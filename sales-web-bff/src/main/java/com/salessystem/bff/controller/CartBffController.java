package com.salessystem.bff.controller;

import com.salessystem.bff.dto.cart.*;
import com.salessystem.bff.client.OrderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("${app.api.path.bff}")
public class CartBffController {

    private static final Logger log = LoggerFactory.getLogger(CartBffController.class);
    private final OrderClient orderClient;

    public CartBffController(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @PostMapping("/cart/items")
    public ResponseEntity<CartResponseDTO> items(@RequestBody AddItemBffRequestDTO request) {
        // FUTURE IMPLEMENTATION: At this point need to retrieve the Authorization Header and read the claim githubId.
        // With the githubId to the future customer-register-service to get the ClientID parameter used to identify
        // the customer owning the cart and pass forward in the order-service call

        // Generating a random customerId because register-service is not implemented
        Integer provisionalCustomerId = ThreadLocalRandom.current().nextInt(100000, 800000);

        AddItemRequestDTO orderSRequest = new AddItemRequestDTO(request.getUuid(), provisionalCustomerId,
                request.getProductId(), request.getQuantity());

        log.info("=== [BFF Gateway] Add item request received from Frontend. UUID: {}", request.getUuid());

        // Routing the call transparently to order-service via OpenFeign
        CartResponseDTO cartResponseDTO = orderClient.addItemToCart(orderSRequest);

        log.info(" -> Success: Add item triggered successfully via Feign.");
        return ResponseEntity.status(HttpStatus.CREATED).body(cartResponseDTO);
    }

    @PostMapping("/cart/checkout")
    public ResponseEntity<OrderStatusResponseDTO> checkout(@RequestBody CheckoutBffRequestDTO request) {
        // FUTURE IMPLEMENTATION: At this point need to retrieve the Authorization Header and read the claim githubId.
        // With the githubId to the future customer-register-service to get the ClientID parameter used to identify
        // the customer owning the cart and pass forward in the order-service call

        // Generating a random customerId because register-service is not implemented
        Integer provisionalCustomerId = ThreadLocalRandom.current().nextInt(100000, 800000);

        CheckoutRequestDTO checkoutOrderSRequest = new CheckoutRequestDTO(request.getUuid(), provisionalCustomerId,
                request.getPaymentToken());

        log.info("=== [BFF Gateway] Checkout request received from Frontend. UUID: {}", request.getUuid());

        // Routing the call transparently to order-service via OpenFeign
        OrderStatusResponseDTO orderStatusResponseDTO =  orderClient.confirmCheckout(checkoutOrderSRequest);

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