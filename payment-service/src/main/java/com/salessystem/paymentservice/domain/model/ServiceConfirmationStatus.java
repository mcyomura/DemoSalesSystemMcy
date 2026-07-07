package com.salessystem.paymentservice.domain.model;

public interface ServiceConfirmationStatus {
     // Attributes in an interface are implicitly: public static final
     int DRAFT = 0;
     int PENDING = 1;
     int SUCCESS = 2;
     int FAILED = -1;
     int PAYMENT_REFUNDED = 10;
}
