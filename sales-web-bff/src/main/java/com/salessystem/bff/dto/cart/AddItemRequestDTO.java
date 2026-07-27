package com.salessystem.bff.dto.cart;

/**
 * DTO representing the request payload to add an item in the shopping cart to send to order-service.
 * Located in the infrastructure layer as it handles external web request data.
 */
public class AddItemRequestDTO {
    private String uuid;     // Can be null on the first item addition
    private Integer customerId;  // Can be null while the user is navigating anonymously
    private Integer productId;   // The specific product being added to the cart
    private Integer quantity;    // The quantity of the product

    public AddItemRequestDTO() {}

    public AddItemRequestDTO(String uuid, Integer customerId, Integer productId, Integer quantity) {
        this.uuid = uuid;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}