package com.salessystem.catalogservice.controller;

import com.salessystem.catalogservice.dto.ProductDTO;
import com.salessystem.catalogservice.mapper.ProductMapper;
import com.salessystem.catalogservice.model.Product;
import com.salessystem.catalogservice.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
// Injects the exact property path used in production into the test context environment
@TestPropertySource(properties = "app.api.path.product=/api/v1/products")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ProductMapper productMapper;

    private final String basePath = "/api/v1/products";

    @Test
    @DisplayName("Should return HTTP 200 OK and a paginated list of product DTOs")
    void getProducts_ShouldReturnPaginatedDTOs() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Product product = new Product();
        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(1);
        productDTO.setName("Sample Product");
        productDTO.setPrice(BigDecimal.valueOf(99.90));

        when(productService.getProducts(any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toDTO(any(Product.class))).thenReturn(productDTO);

        // Act & Assert
        mockMvc.perform(get(basePath)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.[0].id").value(1))
                .andExpect(jsonPath("$.content.[0].name").value("Sample Product"))
                .andExpect(jsonPath("$.content.[0].price").value(BigDecimal.valueOf(99.90)))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(productService, times(1)).getProducts(any(Pageable.class));
        verify(productMapper, times(1)).toDTO(any(Product.class));
    }

    @Test
    @DisplayName("Should return HTTP 200 OK and the specific product DTO when found by ID")
    void getProductById_WhenProductExists_ShouldReturnDTO() throws Exception {
        // Arrange
        Integer productId = 1;
        Product product = new Product();

        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(productId);
        productDTO.setName("Specific Product");

        when(productService.getProductById(productId)).thenReturn(product);
        when(productMapper.toDTO(product)).thenReturn(productDTO);

        // Act & Assert
        mockMvc.perform(get(basePath + "/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Specific Product"));

        verify(productService, times(1)).getProductById(productId);
        verify(productMapper, times(1)).toDTO(product);
    }
}