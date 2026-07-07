package com.salessystem.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
//@Profile("dev") // De-coment this for: Topic created by code in Dev only. In production the topic is supposed to be created by a kafka team
// In this demonstration, though, I'm not setting this up, that would require pre-configuration of kakfa to run the demo
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.order-event}")
    private String topicName;

    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name(topicName)
                .partitions(3)
                .replicas(1)
                .build();
    }
}