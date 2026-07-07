package com.salessystem.catalogservice.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Supplier supplier;

    @Column(insertable = false, updatable = false)
    private LocalDateTime created_at;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Inventory inventory;

    public Product() {
    }

    public Product(String name, String description, BigDecimal price, String sku, Supplier supplier) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.sku = sku;
        this.supplier = supplier;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Supplier getSupplier_id() {
        return supplier;
    }

    public void setSupplier_id(Supplier supplier) {
        this.supplier = supplier;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public Inventory getInventory() {   return inventory; }
    public void setInventory(Inventory inventory) {     this.inventory = inventory;  }
}
