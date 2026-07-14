package com.salessystem.catalogservice.integration;

import com.salessystem.catalogservice.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Executes your official database schema and seeds before each test method runs
@Sql(scripts = "/01 DB-CATALOG-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProductControllerIntegrationTest extends BaseIntegrationTest {

    private final String basePath = "/api/v1/products"; // Adjust if your app.api.path.product in properties is different

    @Test
    @DisplayName("Should retrieve default list of products with status 200 OK")
    void getProducts_DefaultPagination_ShouldReturnSuccess() throws Exception {
        // Act & Assert
        mockMvc.perform(get(basePath)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Verify if all 3 products inserted by the SQL script are returned in the default page content
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Mechanical Keyboard RGB"))
                .andExpect(jsonPath("$.content[0].price").value(89.99))
                .andExpect(jsonPath("$.content[1].name").value("Gaming Mouse 16000 DPI"))
                .andExpect(jsonPath("$.content[1].price").value(45.50))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    @DisplayName("Should retrieve paginated list of products based on HTTP parameters")
    void getProducts_WithCustomPageable_ShouldReturnPaginatedSubset() throws Exception {
        // Act & Assert (Requesting page 0 with size 2)
        mockMvc.perform(get(basePath)
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Content should contain exactly 2 elements because of the size limit
                .andExpect(jsonPath("$.content.length()").value(2))
                // The global counter must still know there are 3 total elements in database
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    @DisplayName("Should retrieve paginated list of products based on HTTP parameters")
    void getProducts_WithCustomPageable_ShouldReturnSecondPaginatedSubset() throws Exception {
        // Act & Assert (Requesting page 0 with size 2)
        mockMvc.perform(get(basePath)
                        .param("page", "1")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Content should contain exactly 2 elements because of the size limit
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Premium Cotton T-Shirt Black M"))
                // The global counter must still know there are 3 total elements in database
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    @DisplayName("Should retrieve specific product details when ID is valid")
    void getProductById_WithValidId_ShouldReturnProductDetails() throws Exception {
        // Arrange
        Integer existingProductId = 2; // ID for 'Gaming Mouse 16000 DPI' from the SQL insert

        // Act & Assert
        mockMvc.perform(get(basePath + "/{id}", existingProductId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(existingProductId))
                .andExpect(jsonPath("$.name").value("Gaming Mouse 16000 DPI"))
                .andExpect(jsonPath("$.price").value(45.50));
    }

    @Test
    @DisplayName("Should return 404 Not Found when product ID does not exist")
    void getProductById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Arrange
        Integer nonExistingId = 999;

        // Act & Assert
        mockMvc.perform(get(basePath + "/{id}", nonExistingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}