package com.salessystem.paymentservice.adapters.outbound.persistence;

import com.salessystem.paymentservice.domain.model.PaymentTransaction;

public class PaymentPersistenceMapper {

    // Converts Domain Model into JPA Entity
    public static PaymentEntity toEntity(PaymentTransaction domain) {
        if (domain == null) {
            return null;
        }
        PaymentEntity entity = new PaymentEntity();
        entity.setId(domain.getId());
        entity.setOrderId(domain.getOrderId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setPaymentToken(domain.getPaymentToken());
        entity.setStatus(domain.getStatus());
        // createdAt and updatedAt are omitted here because the Database triggers handle them
        return entity;
    }

    // Converts JPA Entity back into Domain Model
    public static PaymentTransaction toDomain(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentTransaction(
                entity.getId(),
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getTotalAmount(),
                entity.getPaymentToken(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}