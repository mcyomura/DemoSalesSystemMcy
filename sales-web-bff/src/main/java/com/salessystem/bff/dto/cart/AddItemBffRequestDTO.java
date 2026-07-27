package com.salessystem.bff.dto.cart;

/**
 * DTO representing the request payload to add an item in the shopping cart in the BFF.
 * Located in the infrastructure layer as it handles external web request data.
 */
public class AddItemBffRequestDTO {
    private String uuid;     // Can be null on the first item addition
    private Integer productId;   // The specific product being added to the cart
    private Integer quantity;    // The quantity of the product

    public AddItemBffRequestDTO() {}

    public AddItemBffRequestDTO(String uuid, Integer productId, Integer quantity) {
        this.uuid = uuid;
        this.productId = productId;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}