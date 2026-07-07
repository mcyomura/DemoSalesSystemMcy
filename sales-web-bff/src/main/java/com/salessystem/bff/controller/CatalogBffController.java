package com.salessystem.bff.controller;

import com.salessystem.bff.client.CatalogClient;
import com.salessystem.bff.dto.catalog.ProductDetailResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.List;

@RestController
@RequestMapping("${app.api.path.bff}")
public class CatalogBffController {
    private static final Logger log = LoggerFactory.getLogger(CatalogBffController.class);
    private final CatalogClient catalogClient;

    public CatalogBffController(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    /**
     * Fetches a paginated list of products from the catalog microservice and adapts it for the frontend view.
     */
    @GetMapping("/products")
    public ResponseEntity<JsonNode> getCatalogStore(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        log.info("=== [BFF Catalog Gateway] Fetching store product catalog");

        // As the return payload has pageable elements in the json, we are passing it through
        JsonNode rawJsonPayload = catalogClient.getProductList(page, size);

        return ResponseEntity.ok(rawJsonPayload);
    }

    /**
     * Fetches detailed information about a single product context.
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDetailResponseDTO> getProductDetails(@PathVariable("id") Integer id) {
        log.info("=== [BFF Catalog Gateway] Fetching product details for ID: {} ", id);

        ProductDetailResponseDTO prod = catalogClient.getProductById(id);

        return ResponseEntity.ok(prod);
    }
}
