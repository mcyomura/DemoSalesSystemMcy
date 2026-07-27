package com.salessystem.bff.dto.cart;

public class CheckoutBffRequestDTO {
    private String uuid;     // Can be null on the first item addition
    private String paymentToken;

    public CheckoutBffRequestDTO() {}

    public CheckoutBffRequestDTO(String uuid, String paymentToken) {
        this.uuid = uuid;
        this.paymentToken = paymentToken;
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getPaymentToken() { return paymentToken; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }
}