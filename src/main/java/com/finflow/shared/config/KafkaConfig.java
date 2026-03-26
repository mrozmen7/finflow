package com.finflow.shared.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

/**
 * Kafka producer, consumer, and topic configuration.
 * Defined explicitly because Spring Boot's KafkaAutoConfiguration does not
 * activate with {@code spring-boot-starter-webmvc} in Boot 4.x.
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    /** Topic that carries all domain events from the transaction module. */
    public static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";

    /** Topic that carries fraud alert events from the fraud module. */
    public static final String FRAUD_ALERTS_TOPIC = "fraud-alerts";

    /** Dead-letter topic for messages that fail after all retry attempts. */
    public static final String TRANSACTION_EVENTS_DLQ_TOPIC = "transaction-events-dlq";

    /** Retry attempts before a message is sent to the DLQ. */
    private static final long MAX_RETRY_ATTEMPTS = 3L;

    /** Interval between retry attempts in milliseconds. */
    private static final long RETRY_INTERVAL_MS = 1_000L;

    /**
     * Kafka producer factory using bootstrap servers and String serializers.
     */
    @Bean
    public ProducerFactory<String, String> kafkaProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return new DefaultKafkaProducerFactory<>(Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        ));
    }

    /**
     * KafkaTemplate backed by the explicitly configured producer factory.
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(
            ProducerFactory<String, String> kafkaProducerFactory) {
        return new KafkaTemplate<>(kafkaProducerFactory);
    }

    /**
     * Kafka consumer factory using bootstrap servers and String deserializers.
     */
    @Bean
    public ConsumerFactory<String, String> kafkaConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId) {
        return new DefaultKafkaConsumerFactory<>(Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG, groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
    }

    /**
     * Error handler that retries failed messages up to {@value #MAX_RETRY_ATTEMPTS} times
     * with a {@value #RETRY_INTERVAL_MS} ms fixed delay, then routes them to the DLQ topic.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> {
                log.error("Message sent to DLQ after {} failed attempts. Topic: {}, Offset: {}, Error: {}",
                          MAX_RETRY_ATTEMPTS, record.topic(), record.offset(), ex.getMessage());
                return new TopicPartition(TRANSACTION_EVENTS_DLQ_TOPIC, -1);
            }
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_ATTEMPTS));
    }

    /**
     * Listener container factory for {@code @KafkaListener}-annotated methods.
     * Uses the {@link DefaultErrorHandler} for retry and DLQ routing.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> kafkaConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    /**
     * Declares the transaction-events topic. Spring Kafka creates it on startup
     * if it does not already exist.
     */
    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(TRANSACTION_EVENTS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * Declares the fraud-alerts topic. Spring Kafka creates it on startup
     * if it does not already exist.
     */
    @Bean
    public NewTopic fraudAlertsTopic() {
        return TopicBuilder.name(FRAUD_ALERTS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * Declares the dead-letter topic for messages that exhaust all retry attempts.
     */
    @Bean
    public NewTopic transactionEventsDlqTopic() {
        return TopicBuilder.name(TRANSACTION_EVENTS_DLQ_TOPIC)
            .partitions(1)
            .replicas(1)
            .build();
    }
}
