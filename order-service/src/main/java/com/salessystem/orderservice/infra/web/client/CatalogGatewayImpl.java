package com.salessystem.orderservice.infra.web.client;

import com.salessystem.orderservice.application.gateway.ProductGateway;
import com.salessystem.orderservice.domain.Product;
import org.springframework.stereotype.Component;

@Component
public class CatalogGatewayImpl implements ProductGateway {

    private final CatalogClient catalogClient;

    public CatalogGatewayImpl(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @Override
    public Product getProductById(Integer id) {
        // Calls the Feign client
        CatalogClient.ProductResponse response = catalogClient.getProductById(id);

        // Maps the infrastructure DTO to our clean Domain Record
        return new Product(response.getId(), response.getName(), response.getPrice());
    }
}