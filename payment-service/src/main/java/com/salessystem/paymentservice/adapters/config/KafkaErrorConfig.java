package com.salessystem.paymentservice.adapters.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorConfig.class);

    /**
     * Confirms a global error handler for all Kafka Listeners.
     * It retries processing and, upon definitive failure, routes the bad message to a DLQ topic.
     */
    @Bean
    public CommonErrorHandler globalErrorHandler(KafkaTemplate<Object, Object> template) {
        log.info("=== [Kafka Config] Registering Global Dead Letter Queue (DLQ) Error Handler ===");

        // 1. Specifies where the dead letters should be published (automatically creates topic-name + ".DLT")
        var recoverer = new DeadLetterPublishingRecoverer(template);

        // 2. Defines retry logic: Try 3 times, waiting 2000ms (2 seconds) between each attempt
        var backOff = new FixedBackOff(2000L, 2L);

        var errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Optional: You can specify list of exceptions that should NOT be retried (e.g., NullPointerException)
        errorHandler.addNotRetryableExceptions(NullPointerException.class);

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.error("!!! [Kafka Error Monitor] Processing failed on topic: {} | Partition: {} | Offset: {}",
                    record.topic(), record.partition(), record.offset());
            log.error(" -> Error Cause: {}", ex.getMessage());
            log.error(" -> Attempt: {} of 3. Preparing next step...", deliveryAttempt);
        });

        return errorHandler;
    }
}