package com.salessystem.bff.client;

import com.salessystem.bff.dto.cart.AddItemRequestDTO;
import com.salessystem.bff.dto.cart.CartResponseDTO;
import com.salessystem.bff.dto.cart.CheckoutRequestDTO;
import com.salessystem.bff.dto.cart.OrderStatusResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service", url = "${app.services.order.url}", path = "${app.services.order.path}")
public interface OrderClient {
    // Routes to the order-service endpoint that appends items to a live cart
    @PostMapping("/items")
    CartResponseDTO addItemToCart(@RequestBody AddItemRequestDTO itemRequest);

    // Routes to the order-service endpoint that triggers the checkout Saga placement
    @PostMapping("/checkout")
    OrderStatusResponseDTO confirmCheckout(@RequestBody CheckoutRequestDTO checkoutRequest);

    // Routes to the order-service endpoint that consults an order
    @GetMapping("/{id}")
    OrderStatusResponseDTO consultOrder(@PathVariable("id") Integer id);

}