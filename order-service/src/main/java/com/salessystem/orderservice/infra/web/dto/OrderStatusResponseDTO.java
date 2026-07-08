package com.salessystem.orderservice.infra.web.dto;

import java.math.BigDecimal;

public class OrderStatusResponseDTO {
    private Integer id;
    private Integer customerId;
    private String status;
    private String inventory_status;
    private String payment_status;
    private BigDecimal totalAmount;

    public OrderStatusResponseDTO() {
    }

    public OrderStatusResponseDTO(Integer id, Integer customerId, String status, String inventory_status,
                                  String payment_status, BigDecimal totalAmount) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.inventory_status = inventory_status;
        this.payment_status = payment_status;
        this.totalAmount = totalAmount;
    }

    public Integer getId() {  return id;}
    public void setId(Integer id) {  this.id = id;}

    public Integer getCustomerId() {  return customerId;  }
    public void setCustomerId(Integer customerId) { this.customerId = customerId;  }

    public String getStatus() {  return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInventory_status() {  return inventory_status; }
    public void setInventory_status(String inventory_status) { this.inventory_status = inventory_status;  }

    public String getPayment_status() {  return payment_status;  }
    public void setPayment_status(String payment_status) {  this.payment_status = payment_status;  }

    public BigDecimal getTotalAmount() {  return totalAmount;  }
    public void setTotalAmount(BigDecimal totalAmount) {  this.totalAmount = totalAmount; }
}
