package com.salessystem.paymentservice.domain.model;

public class PaymentValidatedEventDTO {
    private Integer orderId;
    private String serviceStatus;
    private String reason; // Optional error description

    // Default constructor for Jackson serialization
    public PaymentValidatedEventDTO() {
    }

    // Custom constructor to build the payload easily
    public PaymentValidatedEventDTO(Integer orderId, String serviceStatus, String reason) {
        this.orderId = orderId;
        this.serviceStatus = serviceStatus;
        this.reason = reason;
    }
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public String getServiceStatus() { return serviceStatus; }
    public void setServiceStatus(String serviceStatus) { this.serviceStatus = serviceStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
