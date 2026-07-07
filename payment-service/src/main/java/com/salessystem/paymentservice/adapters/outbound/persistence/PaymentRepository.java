package com.salessystem.paymentservice.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    // Spring Data automatically generates the query based on method name
    Optional<PaymentEntity> findByOrderId(Integer orderId);
}