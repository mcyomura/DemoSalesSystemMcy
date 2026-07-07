package com.salessystem.catalogservice.repository;

import com.salessystem.catalogservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository <Inventory, Integer> {
    //Finds the inventory tracking record by the associated Product's ID with Optional
    Optional<Inventory> findByProductId(Integer productId);
}
