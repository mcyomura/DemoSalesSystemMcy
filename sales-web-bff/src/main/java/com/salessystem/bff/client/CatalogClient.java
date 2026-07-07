package com.salessystem.bff.client;

import com.salessystem.bff.dto.catalog.ProductDetailResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tools.jackson.databind.JsonNode;

import java.util.List;

@FeignClient(name = "catalog-service", url = "${app.services.catalog.url}", path = "${app.services.catalog.path}")
public interface CatalogClient {
    // Feign will automatically translate this call into an HTTP POST request to the target microservice
    @GetMapping
    JsonNode getProductList(@RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size);

    @GetMapping("/{id}")
    ProductDetailResponseDTO getProductById(@PathVariable("id") Integer id);

}
