package com.salessystem.catalogservice.dto;

public class InventoryValidatedEventDTO {
    private Integer orderId;
    private String serviceStatus; // SUCCESS or REJECTED
    private String reason; // Optional error description

    // Default constructor for Jackson serialization
    public InventoryValidatedEventDTO() {
    }

    // Custom constructor to build the payload easily
    public InventoryValidatedEventDTO(Integer orderId, String serviceStatus, String reason) {
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
