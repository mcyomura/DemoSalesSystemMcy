package com.salessystem.customerauthservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salessystem.customerauthservice.dto.AuthRequest;
import com.salessystem.customerauthservice.dto.GithubTokenResponse;
import com.salessystem.customerauthservice.dto.GithubUserResponse;
import com.salessystem.customerauthservice.model.User;
import com.salessystem.customerauthservice.repository.UserRepository;
import com.salessystem.customerauthservice.service.GithubApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for Customer Auth Service endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.api.path.auth=/api/v1/customerauth",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:04-DB-CUSTOMER-AUTH-test.sql"
})
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Mocking external HTTP gateway to GitHub while keeping Spring context and DB real
    @MockitoBean
    private GithubApiService githubApiService;

    @Test
    @DisplayName("Should execute full authentication flow, persist user in DB, and return valid JWT")
    @Sql(scripts = "/04-DB-CUSTOMER-AUTH-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldAuthenticateWithGithubAndPersistUser() throws Exception {
        // Arrange
        String code = "valid_integration_code";
        AuthRequest authRequest = new AuthRequest(code);

        GithubTokenResponse mockTokenResponse = new GithubTokenResponse();
        mockTokenResponse.setAccessToken("gho_integration_access_token");

        GithubUserResponse mockUserResponse = new GithubUserResponse();
        mockUserResponse.setId(998877L);
        mockUserResponse.setLogin("integration_user");
        mockUserResponse.setName("Integration User");
        mockUserResponse.setEmail("integration@test.com");
        mockUserResponse.setAvatarUrl("https://github.com/avatar/integration.png");

        when(githubApiService.getAccessToken(code)).thenReturn(mockTokenResponse);
        when(githubApiService.getUserProfile("gho_integration_access_token")).thenReturn(mockUserResponse);

        // Act & Assert HTTP Response
        mockMvc.perform(post("/api/v1/customerauth/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("integration@test.com"))
                .andExpect(jsonPath("$.fullName").value("Integration User"));

        // Verify Database Persistence
        Optional<User> savedUserOpt = userRepository.findByGithubId("998877");
        assertTrue(savedUserOpt.isPresent());
        User savedUser = savedUserOpt.get();
        assertEquals("integration@test.com", savedUser.getEmail());
        assertEquals("Integration User", savedUser.getFullName());
    }

    @Test
    @DisplayName("Should return active Base64 RSA public key string via public endpoint")
    void shouldExposeActivePublicKey() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/customerauth/public-key"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }
}