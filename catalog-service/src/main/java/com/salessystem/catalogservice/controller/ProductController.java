package com.salessystem.catalogservice.controller;


import com.salessystem.catalogservice.dto.ProductDTO;
import com.salessystem.catalogservice.mapper.ProductMapper;
import com.salessystem.catalogservice.model.Product;
import com.salessystem.catalogservice.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping ("${app.api.path.product}")
public class ProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;  // MapStruct

    // Dependency Injection
    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getProducts(@PageableDefault(page = 0, size = 10) Pageable pageable){
        Page<Product> products = productService.getProducts(pageable);

        Page<ProductDTO> productDTOs = products.map(productMapper::toDTO);

        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Integer id){
        Product product = productService.getProductById(id);

        ProductDTO productDTO = productMapper.toDTO(product);

        return ResponseEntity.ok(productDTO);
    }
}
