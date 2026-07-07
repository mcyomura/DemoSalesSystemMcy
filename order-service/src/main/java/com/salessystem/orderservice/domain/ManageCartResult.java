package com.salessystem.orderservice.domain;

public record ManageCartResult (
    Order order,
    boolean pricesUpdated
) {}
