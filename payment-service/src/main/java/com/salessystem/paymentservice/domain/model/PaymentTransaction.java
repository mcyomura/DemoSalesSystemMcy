package com.salessystem.paymentservice.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentTransaction {
    private Integer id;
    private Integer orderId;
    private Integer customerId;
    private BigDecimal totalAmount;
    private String paymentToken;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public PaymentTransaction() {
    }

    // Full constructor
    public PaymentTransaction(Integer id, Integer orderId, Integer customerId, BigDecimal totalAmount,
                              String paymentToken, Integer status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.paymentToken = paymentToken;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentToken() { return paymentToken; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}