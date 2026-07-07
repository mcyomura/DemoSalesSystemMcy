package com.salessystem.orderservice.application.usecase;

import com.salessystem.orderservice.application.exception.IllegalOrderStateException;
import com.salessystem.orderservice.application.exception.ResourceNotFoundException;
import com.salessystem.orderservice.application.gateway.OrderGateway;
import com.salessystem.orderservice.domain.ManageCartResult;
import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.domain.OrderItem;
import com.salessystem.orderservice.domain.OrderStatus;
import com.salessystem.orderservice.infra.web.client.CatalogClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Core Use Case responsible for managing the shopping cart lifecycle.
 * Completely decoupled from infrastructure frameworks and HTTP components.
 */
@Service
public class ManageCartUseCase {

    private final OrderGateway orderGateway;
    private final CatalogClient catalogClient;
    private final long cartRefreshMinutes; // Maximum minutes allowed before prices in a cart are refreshed

    // Constructor injection for the repository gateway interface
    public ManageCartUseCase(OrderGateway orderGateway, CatalogClient catalogClient,
                             @Value("${cart.refresh.minutes}") long cartRefreshMinutes) {
        this.orderGateway = orderGateway;
        this.catalogClient = catalogClient;
        this.cartRefreshMinutes = cartRefreshMinutes;
    }

    // Creates a new cart
    private Order createNewCart (Integer customerId) {
        Order order = new Order();

        order.setUuid(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.DRAFT);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPriceUpdatedAt(LocalDateTime.now());

        if (customerId != null) {
            order.setCustomerId(customerId);
        }

        return order;
    }

    // Validate if it is time to updated prices in the cart
    private boolean isPriceStaled(LocalDateTime priceUpdatedAt) {
        // Using the dynamic variable injected from your config layer
        long minutesPassed = ChronoUnit.MINUTES.between(priceUpdatedAt, LocalDateTime.now());

        return minutesPassed >= this.cartRefreshMinutes;
    }

    // Update the price of each item in cart and total the amount
    private void updatePricesInCart(Order order) {
        BigDecimal totalOrderAmount = BigDecimal.ZERO;

        // Validates every item against the catalog-service
        for (OrderItem item : order.getItems()) {
            // 1. collateral call
            CatalogClient.ProductResponse product = catalogClient.getProductById(item.getProductId());

            // 2. Updates the staled price
            item.setPriceAtPurchase(product.getPrice());

            // 3. Calculates total amount
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalOrderAmount = totalOrderAmount.add(itemTotal);

            order.setTotalAmount(totalOrderAmount);
        }
    }

    // Executes the business logic to add a single item to the cart.
    // Safely handles new sessions, existing open carts, and expired/completed cart UUIDs.
    public ManageCartResult executeAddItem(String cartUuid, Integer customerId, Integer productId, Integer quantity) {
        Order order = null;
        boolean pricesUpdated = false;

        if (cartUuid == null) {    // new cart
            order = createNewCart(customerId);
        } else {    //ongoing cart
            Optional<Order> orderOpt = orderGateway.findByUuid(cartUuid);
            if (orderOpt.isEmpty()){
                throw new ResourceNotFoundException("UUID not found:" + cartUuid);
            }
            order = orderOpt.get();
            if (order.getStatus().getCode() != OrderStatus.DRAFT.getCode()){
                throw new IllegalOrderStateException("Shopping cart already closed. UUID:" + cartUuid);
            }

            // Check if prices must be refreshed (lasted refresh more than X configured hours)
            if (isPriceStaled(order.getPriceUpdatedAt())) {
                updatePricesInCart(order);
                order.setPriceUpdatedAt(LocalDateTime.now());
                pricesUpdated = true;
            }

            // If user logged in during the active session, bind the customerId
            if (customerId != null) {
                order.setCustomerId(customerId);
            }
        }

        // Retrieves price for the new item
        CatalogClient.ProductResponse product = catalogClient.getProductById(productId);
        // add the new item
        order.addItem(productId, quantity, product.getPrice());

        // updates total amount
        BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal totalAmount = order.getTotalAmount().add(itemTotal);
        order.setTotalAmount(totalAmount);

        // Save the consolidated state and return the updated Domain Entity
        Order savedOrder =orderGateway.save(order);

        return new ManageCartResult(savedOrder, pricesUpdated);
    }
}