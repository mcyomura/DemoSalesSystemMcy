package com.salessystem.orderservice.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {

    private Integer id;
    private Long version;
    private Integer customerId;
    private String uuid;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private SagaStatus inventoryStatus;
    private SagaStatus paymentStatus;
    private LocalDateTime priceUpdatedAt;
    private LocalDateTime updatedAt;
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version;   }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid;  }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public SagaStatus getInventoryStatus() { return inventoryStatus;  }
    public void setInventoryStatus(SagaStatus inventoryStatus) { this.inventoryStatus = inventoryStatus;  }

    public SagaStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(SagaStatus paymentStatus) { this.paymentStatus = paymentStatus;  }

    public LocalDateTime getPriceUpdatedAt() { return priceUpdatedAt; }
    public void setPriceUpdatedAt(LocalDateTime priceUpdatedAt) { this.priceUpdatedAt = priceUpdatedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    /**
     * Adds a single product item to the cart.
     * If the product already exists in the cart, it increments its quantity.
     * If it's a new product, it creates a new OrderItem and links it to this order.
     */
    public void addItem(Integer productId, Integer quantity, BigDecimal priceAtPurchase) {
        if (productId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Invalid product ID or quantity");
        }

        // 1. Search if the product is already present in the current cart items
        OrderItem existingItem = this.items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Scenario A: Product already exists, just increment the quantity
            int newQuantity = existingItem.getQuantity() + quantity;
            existingItem.setQuantity(newQuantity);
        } else {
            // Scenario B: New product for this cart, create a new item link
            OrderItem newItem = new OrderItem();
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            newItem.setOrder(this); // Crucial for JPA Bi-directional mapping (Foreign Key)

            newItem.setPriceAtPurchase(priceAtPurchase);

            this.items.add(newItem);
        }
    }
}