package com.salessystem.bff.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public class CheckoutRequestDTO {
    private String uuid;     // Can be null on the first item addition
    private Integer customerId;  // Can be null while the user is navigating anonymously
    private String paymentToken;
    //private String bearerToken;

    public CheckoutRequestDTO() {}

    public CheckoutRequestDTO(String uuid, Integer customerId, String paymentToken) {
        this.uuid = uuid;
        this.customerId = customerId;
        this.paymentToken = paymentToken;
        //this.bearerToken = bearerToken;
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getPaymentToken() { return paymentToken; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }

    //public String getBearerToken() { return bearerToken; }
    //public void setBearerToken(String bearerToken) {  this.bearerToken = bearerToken;  }
}