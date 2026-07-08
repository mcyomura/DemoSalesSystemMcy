package com.salessystem.orderservice.infra.persistence.entity;

import com.salessystem.orderservice.domain.OrderStatus;
import com.salessystem.orderservice.domain.SagaStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Version;

@Entity
@Table(name = "orders")
public class OrderDbEntity {
    @Version
    private Long version;   // JPA controls this field, with this we control the concurrency between stock and payment status update

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(nullable = false, unique = true, length = 50)
    private String uuid;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "inventory_status")
    @Enumerated(EnumType.STRING)
    private SagaStatus inventoryStatus;

    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private SagaStatus paymentStatus;

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

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public SagaStatus getInventoryStatus() { return inventoryStatus;   }
    public void setInventoryStatus(SagaStatus inventoryStatus) {   this.inventoryStatus = inventoryStatus;  }

    public SagaStatus getPaymentStatus() { return paymentStatus;}
    public void setPaymentStatus(SagaStatus paymentStatus) { this.paymentStatus = paymentStatus; }

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