package com.salessystem.catalogservice.service;

import com.salessystem.catalogservice.exception.ProductNotFoundException;
import com.salessystem.catalogservice.model.Product;
import com.salessystem.catalogservice.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    // Dependency Injection via Constructor
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<Product> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Product getProductById (Integer id){
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id:"+ id));
    }
}
