package com.salessystem.catalogservice.repository;

import com.salessystem.catalogservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository <Product, Integer> {
    // Customized query by SKU
    Optional<Product> findBySku(String sku);
}
