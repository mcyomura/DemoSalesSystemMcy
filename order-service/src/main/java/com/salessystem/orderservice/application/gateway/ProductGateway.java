package com.salessystem.orderservice.application.gateway;

import com.salessystem.orderservice.domain.Product;

public interface ProductGateway {
    Product getProductById(Integer id);
}