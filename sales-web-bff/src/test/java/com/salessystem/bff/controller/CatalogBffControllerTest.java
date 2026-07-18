package com.salessystem.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salessystem.bff.client.CatalogClient;
import com.salessystem.bff.dto.catalog.ProductDetailResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogBffController.class)
@TestPropertySource(properties = "app.api.path.bff=/api/v1/salesbff")
class CatalogBffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Using the modern Spring Boot pattern instead of legacy @MockBean
    @MockitoBean
    private CatalogClient catalogClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should return paginated products successfully")
    void getCatalogStore_ShouldReturnProducts() throws Exception {
        // Arrange: Mocking the custom tools.jackson.databind.JsonNode
        JsonNode mockResponse = Mockito.mock(JsonNode.class);

        given(catalogClient.getProductList(0, 10)).willReturn(mockResponse);

        // Act & Assert: Simulate the HTTP GET request and verify results
        mockMvc.perform(get("/api/v1/salesbff/products") // Adjusted path assuming the property expands correctly in test environment
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return single product details successfully")
    void getProductDetails_ShouldReturnProductProductDetail() throws Exception {
        // Arrange: Create a mock response DTO
        Integer productId = 1;
        ProductDetailResponseDTO mockProduct = new ProductDetailResponseDTO();

        given(catalogClient.getProductById(productId)).willReturn(mockProduct);

        // Act & Assert: Simulate HTTP GET for a single product ID
        mockMvc.perform(get("/api/v1/salesbff/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}