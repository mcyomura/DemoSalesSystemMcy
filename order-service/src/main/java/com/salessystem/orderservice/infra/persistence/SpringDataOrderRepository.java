package com.salessystem.orderservice.infra.persistence;

import com.salessystem.orderservice.infra.persistence.entity.OrderDbEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataOrderRepository extends JpaRepository<OrderDbEntity, Integer> {
    Optional<OrderDbEntity> findByUuid(String uuid);
}
