package com.oms.orderservice.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    /* =========================
       PRODUCER CONFIG
    ========================= */

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        log.info("📦 Kafka Producer connecting to {}", bootstrapServers);

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        log.info("📤 KafkaTemplate initialized");
        return new KafkaTemplate<>(producerFactory());
    }

    /* =========================
       CONSUMER CONFIG
    ========================= */

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        log.info("📥 Kafka Consumer connecting to {}", bootstrapServers);
        log.info("👥 Kafka Consumer Group = {}", consumerGroupId);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /* =========================
       LISTENER FACTORY
    ========================= */

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    kafkaListenerContainerFactory() {

        log.info("🎧 KafkaListenerContainerFactory created");

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // 🔥 This logs partition assignment — proves consumer is live
        factory.getContainerProperties().setConsumerRebalanceListener(
                new ConsumerAwareRebalanceListener() {
                    @Override
                    public void onPartitionsAssigned(
                            org.apache.kafka.clients.consumer.Consumer<?, ?> consumer,
                            java.util.Collection<org.apache.kafka.common.TopicPartition> partitions
                    ) {
                        log.info("✅ Kafka consumer assigned partitions: {}", partitions);
                    }
                }
        );

        return factory;
    }
}
