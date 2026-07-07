package com.salessystem.orderservice.infra.persistence;

import com.salessystem.orderservice.application.gateway.OrderGateway;
import com.salessystem.orderservice.domain.Order;
import com.salessystem.orderservice.infra.persistence.entity.OrderDbEntity;
import com.salessystem.orderservice.infra.persistence.mapper.OrderMapper;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class OrderRepositoryGateway implements OrderGateway {

    private final SpringDataOrderRepository repository;
    private final OrderMapper mapper;

    // Constructor injection
    public OrderRepositoryGateway(SpringDataOrderRepository repository, OrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Bridges the call to the Spring Data JPA repository to find an order by its numerical ID.
     */
    public Optional<Order> findById(Integer id) {
        // Calling the automatic findById from JpaRepository
        Optional<OrderDbEntity> entityOptional = repository.findById(id);

        return entityOptional.map(entity -> mapper.toDomainOrder(entity));
    }

    /**
     * Implementing the missing method from OrderGateway interface.
     * This will fix the "is not abstract and does not override" compile error.
     */
    @Override
    public Optional<Order> findByUuid(String uuid) {
        // 1. Fetch from database using Spring Data repository
        // 2. Map the infrastructure entity back to the Domain Order
        // (If your Spring Data repository already returns the Domain Order, just return it directly)

        Optional<OrderDbEntity> entityOptional = repository.findByUuid(uuid);

        return entityOptional.map(entity -> mapper.toDomainOrder(entity));
    }

    @Override
    public Order save(Order orderDomain) {
        // 1. Converts domain to DB entity
        OrderDbEntity entityToSave = mapper.toEntityOrder(orderDomain);

        // 2. Saves with Jpa
        OrderDbEntity savedEntity = repository.save(entityToSave);

        // 3. Gets the db entity and converts again to domain - has the ID, created_at and updated_ap created when saving
        return mapper.toDomainOrder(savedEntity);
    }
}
