package com.salessystem.catalogservice.config;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@EnableKafka
@Configuration
//@Profile("dev") // De-coment this for: Topic created by code in Dev only. In production the topic is supposed to be created by a kafka team
// In this demonstration, though, I'm not setting this up, that would require pre-configuration of kakfa to run the demo
public class KafkaTopicConfig {
    @Value("${app.kafka.topics.inventory-validated}")
    private String inventoryValidatedTopic;

    @Bean
    public NewTopic inventoryValidatedTopic() {
        return TopicBuilder.name(inventoryValidatedTopic)
                .partitions(3) // 3 partitions for scale and concurrent consumers
                .replicas(1)   // 1 replica since we are running locally in dev mode
                .build();
    }

}