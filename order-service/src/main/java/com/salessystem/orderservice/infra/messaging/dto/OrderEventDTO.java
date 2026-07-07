package com.salessystem.orderservice.infra.messaging.dto;

import com.salessystem.orderservice.domain.OrderEventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderEventDTO {
    private OrderEventType orderEventType;
    private Integer orderId;
    private Integer customerId;
    private BigDecimal totalAmount;
    private LocalDateTime updatedAt;
    private List<OrderItemEventDTO> items; // Now the inventory service can deduct the stock!
    private String paymentToken;

    public OrderEventDTO() {
    }

    public OrderEventDTO(OrderEventType orderEventType, Integer orderId, Integer customerId, BigDecimal totalAmount, LocalDateTime updatedAt,
                         List<OrderItemEventDTO> items, String paymentToken) {
        this.orderEventType = orderEventType;
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.updatedAt = updatedAt;
        this.items = items;
        this.paymentToken = paymentToken;
    }

    // Getters and Setters
    public OrderEventType getOrderEventType() { return orderEventType;  }
    public void setOrderEventType(OrderEventType orderEventType) {  this.orderEventType = orderEventType;  }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderItemEventDTO> getItems() { return items; }
    public void setItems(List<OrderItemEventDTO> items) { this.items = items; }

    public String getPaymentToken() {     return paymentToken;  }
    public void setPaymentToken(String paymentToken) {  this.paymentToken = paymentToken; }

    public static class OrderItemEventDTO {
        private Integer productId;
        private Integer quantity;

        public OrderItemEventDTO() {
        }

        public OrderItemEventDTO(Integer productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}