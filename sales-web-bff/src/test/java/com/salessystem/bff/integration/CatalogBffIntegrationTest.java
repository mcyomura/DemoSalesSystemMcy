package com.salessystem.bff.integration;

import com.salessystem.bff.client.CatalogClient;
import com.salessystem.bff.dto.catalog.ProductDetailResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.api.path.bff=/api/v1/salesbff")
@AutoConfigureMockMvc
class CatalogBffIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Mocking the external Feign client gateway to isolate network calls to catalog-service
    @MockitoBean
    private CatalogClient catalogClient;

    @Test
    @DisplayName("Should route product list request through BFF and return data successfully")
    void shouldRouteAndReturnProductList() throws Exception {
        // Arrange
        int page = 0;
        int size = 5;

        // Simulating the tools.jackson.databind.JsonNode response payload
        JsonNode mockJsonNode = Mockito.mock(JsonNode.class);
        when(catalogClient.getProductList(page, size)).thenReturn(mockJsonNode);

        // Act & Assert
        mockMvc.perform(get("/api/v1/salesbff/products")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should route product details request through BFF and map fields correctly")
    void shouldRouteAndReturnProductDetails() throws Exception {
        // Arrange
        Integer productId = 1;

        ProductDetailResponseDTO mockProduct = new ProductDetailResponseDTO();
        mockProduct.setId(1);
        mockProduct.setName("Mechanical Keyboard RGB");
        mockProduct.setSku("TECH-KEYB-RGB-BR");
        mockProduct.setDescription("Wireless mechanical keyboard with brown switches");
        mockProduct.setPrice(BigDecimal.valueOf(89.99));
        mockProduct.setQuantityInStock(45);
        mockProduct.setSupplierName("Tech Components Ltd");

        when(catalogClient.getProductById(productId)).thenReturn(mockProduct);

        // Act & Assert
        mockMvc.perform(get("/api/v1/salesbff/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard RGB"))
                .andExpect(jsonPath("$.sku").value("TECH-KEYB-RGB-BR"))
                .andExpect(jsonPath("$.description").value("Wireless mechanical keyboard with brown switches"))
                .andExpect(jsonPath("$.price").value("89.99"))
                .andExpect(jsonPath("$.quantityInStock").value(45))
                .andExpect(jsonPath("$.supplierName").value("Tech Components Ltd"));
    }
}