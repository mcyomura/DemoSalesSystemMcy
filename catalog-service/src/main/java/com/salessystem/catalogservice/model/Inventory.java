package com.salessystem.catalogservice.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 100)
    private String warehouse_location;

    @Column(insertable = false, updatable = false)
    private LocalDateTime last_updated;
/*
    @OneToOne
    @JoinColumn(name = "product_id") // FK
    private Product product;
*/
    public Inventory() {
    }

    public Inventory(Product product, Integer quantity, String warehouse_location) {
        this.product = product;
        this.quantity = quantity;
        this.warehouse_location = warehouse_location;
    }

    public Integer getId() {
        return id;
    }

    public Product getProduct_id() {
        return product;
    }

    public void setProduct_id(Product product_id) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getWarehouse_location() {
        return warehouse_location;
    }

    public void setWarehouse_location(String warehouse_location) {
        this.warehouse_location = warehouse_location;
    }

    public LocalDateTime getLast_updated() {
        return last_updated;
    }
}
