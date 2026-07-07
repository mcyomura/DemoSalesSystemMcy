package com.salessystem.orderservice.infra.web.client;

import com.salessystem.orderservice.infra.web.exception.CatalogFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


import java.math.BigDecimal;

// name of the microservice and URL
@FeignClient(name = "catalog-service",
        url = "${app.services.catalog.url}", path = "${app.services.catalog.path}",
        configuration = CatalogFeignConfig.class)
public interface CatalogClient {

    // a mini-DTO (parcial deserialization - Jackson)
    class ProductResponse {
        private Integer id;
        private String name;
        private BigDecimal price;

        // Getters e Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    @GetMapping("/{id}")
    ProductResponse getProductById(@PathVariable("id") Integer id);
}
