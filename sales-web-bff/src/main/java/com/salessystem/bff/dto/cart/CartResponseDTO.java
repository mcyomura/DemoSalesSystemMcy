package com.salessystem.bff.dto.cart;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class CartResponseDTO {
    private Integer customerId;
    private String uuid;
    private String status;
    private BigDecimal totalAmount;
    private boolean pricesUpdated;
    private List<OrderItemResponseDTO> items;

    // Getters e Setters
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public boolean getPricesUpdated() { return pricesUpdated;   }
    public void setPricesUpdated(boolean pricesUpdated) {  this.pricesUpdated = pricesUpdated;  }
    public List<OrderItemResponseDTO> getItems() { return items; }
    public void setItems(List<OrderItemResponseDTO> items) { this.items = items; }

    public static class OrderItemResponseDTO {
        private Integer productId;
        private Integer quantity;
        private BigDecimal unitaryPriceAtCart;

        // Getters e Setters
        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitaryPriceAtCart() {     return unitaryPriceAtCart;     }
        public void setUnitaryPriceAtCart(BigDecimal unitaryPriceAtCart) {  this.unitaryPriceAtCart = unitaryPriceAtCart; }
    }
}