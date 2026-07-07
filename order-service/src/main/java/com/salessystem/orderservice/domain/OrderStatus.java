package com.salessystem.orderservice.domain;

public enum OrderStatus {
    DRAFT(0),
    PENDING(1),
    APPROVED(2),
    CANCELLED(-1),
    ABANDONED(-2);

    private final int code;

    // Constructor (implicitly private)
    OrderStatus(int code) {
        this.code = code;
    }

    // Returns the numeric code for the Database/JPA
    public int getCode() {
        return code;
    }

    // Helper method to reconstruct the Enum from a DB code
    public static OrderStatus fromCode(int code) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus code: " + code);
    }
}