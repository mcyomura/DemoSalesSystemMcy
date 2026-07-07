package com.salessystem.orderservice.infra.messaging.dto;

public class PaymentValidatedEventDTO {
    private Integer orderId;
    private Integer serviceStatus; // SUCCESS or REJECTED
    private String reason; // Optional error description

    // Default constructor for Jackson serialization
    public PaymentValidatedEventDTO() {
    }

    // Custom constructor to build the payload easily
    public PaymentValidatedEventDTO(Integer orderId, Integer serviceStatus, String reason) {
        this.orderId = orderId;
        this.serviceStatus = serviceStatus;
        this.reason = reason;
    }
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public Integer getServiceStatus() { return serviceStatus; }
    public void setServiceStatus(Integer serviceStatus) { this.serviceStatus = serviceStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
