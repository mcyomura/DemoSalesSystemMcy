package com.salessystem.catalogservice.repository;

import com.salessystem.catalogservice.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
}
