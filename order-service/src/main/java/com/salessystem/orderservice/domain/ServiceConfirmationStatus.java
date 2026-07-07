package com.salessystem.orderservice.domain;

public interface ServiceConfirmationStatus {
     // Attributes in an interface are implicitly: public static final
     int DRAFT = 0;
     int PENDING = 1;
     int SUCCESS = 2;
     int FAILED = -1;
     int ROLLBACK = 10; // returned stock or payment refunded
}
