package com.salessystem.orderservice.infra.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderDbEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(nullable = false, unique = true, length = 50)
    private String uuid;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "inventory_status")
    private Integer inventoryStatus;

    @Column(name = "payment_status")
    private Integer paymentStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // This field stores when the prices in this cart were last verified against the Catalog
    @Column(name = "price_updated_at", nullable = false)
    private LocalDateTime priceUpdatedAt;

    @UpdateTimestamp // Value is automatically updated
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemDbEntity> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public OrderDbEntity() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Integer getInventoryStatus() { return inventoryStatus;   }
    public void setInventoryStatus(Integer inventoryStatus) {   this.inventoryStatus = inventoryStatus;  }

    public Integer getPaymentStatus() { return paymentStatus;}
    public void setPaymentStatus(Integer paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setPriceUpdatedAt(LocalDateTime priceUpdatedAt) { this.priceUpdatedAt = priceUpdatedAt; }
    public LocalDateTime getPriceUpdatedAt() { return priceUpdatedAt; }

    public LocalDateTime getUpdatedAt() {  return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) {  this.updatedAt = updatedAt;  }

    public List<OrderItemDbEntity> getItems() { return items; }
    public void setItems(List<OrderItemDbEntity> items) {
        this.items = items;
        if (items != null) {
            items.forEach(item -> item.setOrder(this));
        }
    }
}