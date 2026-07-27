package com.salessystem.bff.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.salessystem.bff.client.CustomerAuthFeignClient;
import com.salessystem.bff.client.OrderClient;
import com.salessystem.bff.dto.cart.AddItemRequestDTO;
import com.salessystem.bff.dto.cart.CartResponseDTO;
import com.salessystem.bff.dto.cart.CheckoutRequestDTO;
import com.salessystem.bff.dto.cart.OrderStatusResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.api.path.bff=/api/v1/salesbff")
@AutoConfigureMockMvc
class CartBffIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Mocking the external Feign client gateway to isolate network calls to order-service
    @MockitoBean
    private OrderClient orderClient;

    @MockitoBean
    private CustomerAuthFeignClient authFeignClient;

    private String validJwtToken;

    @BeforeEach
    void setUp() throws Exception {
        // Generate temporary RSA KeyPair for integration test
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        // Mock public key endpoint response
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        when(authFeignClient.getPublicKey()).thenReturn(ResponseEntity.ok(publicKeyBase64));

        // Generate signed valid JWT token
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("827329")
                .claim("email", "user@test.com")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .build();

        JWSSigner signer = new RSASSASigner((RSAPrivateKey) keyPair.getPrivate());
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
        signedJWT.sign(signer);

        this.validJwtToken = signedJWT.serialize();
    }

    @Test
    @DisplayName("Should route add item request through BFF and return 201 Created")
    void shouldRouteAndAddItemToCart() throws Exception {
        // Arrange
        AddItemRequestDTO requestDto = new AddItemRequestDTO();
        requestDto.setProductId(1);
        requestDto.setQuantity(3);
        requestDto.setUuid( "b1905bcc-0e72-4b0f-8bde-b561e3a5f89c");

        CartResponseDTO mockResponseDto = new CartResponseDTO();
        CartResponseDTO.OrderItemResponseDTO item1 = new CartResponseDTO.OrderItemResponseDTO();
        item1.setProductId(1);
        item1.setQuantity(2);
        item1.setUnitaryPriceAtCart(BigDecimal.valueOf(89.99));
        CartResponseDTO.OrderItemResponseDTO item2 = new CartResponseDTO.OrderItemResponseDTO();
        item2.setProductId(3);
        item2.setQuantity(1);
        item2.setUnitaryPriceAtCart(BigDecimal.valueOf(29.9));
        mockResponseDto.setItems(List.of(item1,item2));
        mockResponseDto.setStatus("DRAFT");
        mockResponseDto.setPricesUpdated(false);
        mockResponseDto.setUuid( "b1905bcc-0e72-4b0f-8bde-b561e3a5f89c");
        mockResponseDto.setTotalAmount(BigDecimal.valueOf(209.88));

        when(orderClient.addItemToCart(any(AddItemRequestDTO.class))).thenReturn(mockResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/salesbff/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value("b1905bcc-0e72-4b0f-8bde-b561e3a5f89c"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(BigDecimal.valueOf(209.88)))
                .andExpect(jsonPath("$.pricesUpdated").value(false))
                .andExpect(jsonPath("$.items.[0].productId").value(1))
                .andExpect(jsonPath("$.items.[0].quantity").value(2))
                .andExpect(jsonPath("$.items.[0].unitaryPriceAtCart").value(BigDecimal.valueOf(89.99)))
                .andExpect(jsonPath("$.items.[1].productId").value(3))
                .andExpect(jsonPath("$.items.[1].quantity").value(1))
                .andExpect(jsonPath("$.items.[1].unitaryPriceAtCart").value(BigDecimal.valueOf(29.9)));
    }

    @Test
    @DisplayName("Should route checkout request through BFF and return 202 Accepted with valid Authorization header")
    void shouldRouteAndConfirmCheckout() throws Exception {
        // Arrange
        CheckoutRequestDTO requestDto = new CheckoutRequestDTO();
        requestDto.setUuid("e02107ef-61d0-41db-a402-406a9375c345");
        requestDto.setCustomerId(827329);
        requestDto.setPaymentToken("jfiej-jiefa-ieicmm88");

        OrderStatusResponseDTO mockResponseDto = new OrderStatusResponseDTO();
        mockResponseDto.setCustomerId(827329);
        mockResponseDto.setId(1);
        mockResponseDto.setStatus("PENDING");
        mockResponseDto.setTotalAmount(BigDecimal.valueOf(209.88));
        mockResponseDto.setPayment_status("PENDING");
        mockResponseDto.setInventory_status("PENDING");

        when(orderClient.confirmCheckout(any(CheckoutRequestDTO.class))).thenReturn(mockResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/salesbff/cart/checkout")
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerId").value(827329))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.inventory_status").value("PENDING"))
                .andExpect(jsonPath("$.payment_status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(BigDecimal.valueOf(209.88)));
    }

    @Test
    @DisplayName("Should route consult order request through BFF and return 202 Accepted with valid Authorization header")
    void shouldRouteAndConsultOrder() throws Exception {
        // Arrange
        Integer orderId = 999;
        OrderStatusResponseDTO mockResponseDto = new OrderStatusResponseDTO();
        mockResponseDto.setCustomerId(827329);
        mockResponseDto.setId(orderId);
        mockResponseDto.setStatus("APPROVED");
        mockResponseDto.setTotalAmount(BigDecimal.valueOf(209.88));
        mockResponseDto.setPayment_status("SUCCESS");
        mockResponseDto.setInventory_status("SUCCESS");

        when(orderClient.consultOrder(orderId)).thenReturn(mockResponseDto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/salesbff/cart/{id}", orderId)
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.customerId").value(827329))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.inventory_status").value("SUCCESS"))
                .andExpect(jsonPath("$.payment_status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalAmount").value(BigDecimal.valueOf(209.88)));
    }
}