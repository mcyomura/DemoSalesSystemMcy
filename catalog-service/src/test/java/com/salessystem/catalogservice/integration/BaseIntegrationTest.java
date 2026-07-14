package com.salessystem.catalogservice.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;

    // MariaDB can keep @ServiceConnection because Spring Data Boot integration is extremely stable
    @Container
    @ServiceConnection // Spring configures url, username and passwd
    protected static final MariaDBContainer<?> mariaDBContainer =
            new MariaDBContainer<>(DockerImageName.parse("mariadb:12.3"));

    // Removed @ServiceConnection from Kafka to avoid Spring Boot 4 compatibility metadata bugs
    @Container
    protected static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    // Manually register Kafka bootstrap servers directly into Spring context properties
    @DynamicPropertySource
    static void registryProperties(DynamicPropertyRegistry registry) {
        // Permit hibernate to create db tables during the test
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        // Kafka's address
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.properties.spring.json.value.default.type",
                () -> "com.salessystem.catalogservice.dto.OrderEventDTO");

        // Trust all packages during the test
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
    }
}
