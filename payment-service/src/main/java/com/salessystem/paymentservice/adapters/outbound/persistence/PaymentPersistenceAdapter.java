package com.salessystem.paymentservice.adapters.outbound.persistence;

import com.salessystem.paymentservice.domain.model.PaymentTransaction;
import com.salessystem.paymentservice.ports.outbound.PaymentRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class PaymentPersistenceAdapter implements PaymentRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(PaymentPersistenceAdapter.class);
    private final PaymentRepository repository;

    public PaymentPersistenceAdapter(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentTransaction save(PaymentTransaction paymentTransaction) {
        log.info("Mapping and saving payment transaction for Order ID: {}", paymentTransaction.getOrderId());

        // 1. Convert domain object to JPA Entity
        PaymentEntity entity = PaymentPersistenceMapper.toEntity(paymentTransaction);

        // 2. Save via Spring Data JPA
        PaymentEntity savedEntity = repository.save(entity);

        // 3. Convert back to domain and return
        return PaymentPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PaymentTransaction> findByOrderId(Integer orderId) {
        log.info("Searching payment record for Order ID: {}", orderId);
        return repository.findByOrderId(orderId)
                .map(PaymentPersistenceMapper::toDomain);
    }
}