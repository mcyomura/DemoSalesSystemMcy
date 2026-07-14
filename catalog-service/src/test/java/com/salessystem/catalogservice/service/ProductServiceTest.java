package com.salessystem.catalogservice.service;

import com.salessystem.catalogservice.exception.ProductNotFoundException;
import com.salessystem.catalogservice.model.Product;
import com.salessystem.catalogservice.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Should return a paginated list of products successfully")
    void getProducts_ShouldReturnPaginatedList() {
        // Arrange
        // pageNumber 1 is the second page, since 0 is the first
        Pageable pageable = PageRequest.of(0, 5);

        Product product1 = new Product();
        ReflectionTestUtils.setField(product1, "id", 1);
        Product product2 = new Product();
        ReflectionTestUtils.setField(product2, "id", 2);


        Page<Product> mockPage = new PageImpl<>(List.of(product1, product2), pageable, 2);

        when(productRepository.findAll(pageable)).thenReturn(mockPage);

        // Act
        Page<Product> result = productService.getProducts(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // nr of elements returned in the current page
        assertThat(result.getTotalElements()).isEqualTo(2); // total elements in the "database"
        verify(productRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should return the specific product when found by ID")
    void getProductById_WhenProductExists_ShouldReturnProduct() {
        // Arrange
        Integer productId = 42;
        Product mockProduct = new Product();
        ReflectionTestUtils.setField(mockProduct, "id", productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

        // Act
        Product result = productService.getProductById(productId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId);
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when product does not exist")
    void getProductById_WhenProductDoesNotExist_ShouldThrowException() {
        // Arrange
        Integer productId = 99;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found with id:" + productId);

        verify(productRepository, times(1)).findById(productId);
    }
}