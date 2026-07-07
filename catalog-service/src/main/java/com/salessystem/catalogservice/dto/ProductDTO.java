package com.salessystem.catalogservice.dto;

import java.math.BigDecimal;

public class ProductDTO {

    private Integer id;
    private String name;
    private String description;
    private BigDecimal price;
    private String sku;
    private String supplierName; // to receive the supplier name from the other table
    private Integer quantityInStock;

    // Empty constructor (mandatory)
    public ProductDTO() {}

    // Getters & Setters (MapStruct use to inject data)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public Integer getQuantityInStock() {   return quantityInStock;    }
    public void setQuantityInStock(Integer quantityInStock) {   this.quantityInStock = quantityInStock;    }
}