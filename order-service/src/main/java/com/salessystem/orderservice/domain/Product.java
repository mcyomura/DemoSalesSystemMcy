package com.salessystem.orderservice.domain;

import java.math.BigDecimal;

/**
 * Domain representation of a Product, keeping the core decoupled from infrastructure DTOs.
 */
public record Product(Integer id, String name, BigDecimal price) {}