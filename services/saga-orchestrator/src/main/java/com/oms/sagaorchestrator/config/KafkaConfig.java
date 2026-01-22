package com.oms.sagaorchestrator.config;

import com.oms.eventcontracts.events.InventoryReservedEvent;
import com.oms.eventcontracts.events.InventoryUnavailableEvent;
import com.oms.eventcontracts.events.OrderCreatedEvent;
import com.oms.eventcontracts.events.PaymentCompletedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

        @Value("${spring.kafka.bootstrap-servers}")
        private String bootstrapServers;

        @Value("${kafka.consumer.group-id}")
        private String groupId;

        @Bean
        public DefaultErrorHandler sagaErrorHandler(
                        DeadLetterPublishingRecoverer recoverer) {
                FixedBackOff backOff = new FixedBackOff(2000L, 3); // 3 retries
                return new DefaultErrorHandler(recoverer, backOff);
        }

        @Bean
        public ConsumerFactory<String, OrderCreatedEvent> consumerFactory() {
                Map<String, Object> props = new HashMap<>();

                props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
                props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

                JsonDeserializer<OrderCreatedEvent> deserializer = new JsonDeserializer<>(OrderCreatedEvent.class);

                deserializer.addTrustedPackages("com.oms.eventcontracts");
                deserializer.setUseTypeHeaders(false);

                return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> kafkaListenerContainerFactory(
                        DefaultErrorHandler sagaErrorHandler) {
                ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory());
                factory.setCommonErrorHandler(sagaErrorHandler);
                factory.getContainerProperties()
                                .setAckMode(ContainerProperties.AckMode.RECORD);

                return factory;
        }

        @Bean
        public ProducerFactory<String, Object> producerFactory() {
                Map<String, Object> config = new HashMap<>();
                config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

                return new DefaultKafkaProducerFactory<>(config);
        }

        @Bean
        public KafkaTemplate<String, Object> kafkaTemplate() {
                return new KafkaTemplate<>(producerFactory());
        }

        @Bean
        public ConsumerFactory<String, PaymentCompletedEvent> paymentCompletedConsumerFactory() {

                Map<String, Object> props = new HashMap<>();
                props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
                props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

                JsonDeserializer<PaymentCompletedEvent> deserializer = new JsonDeserializer<>(
                                PaymentCompletedEvent.class);

                deserializer.addTrustedPackages("com.oms.eventcontracts");
                deserializer.setUseTypeHeaders(false);

                return new DefaultKafkaConsumerFactory<>(
                                props,
                                new StringDeserializer(),
                                deserializer);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> paymentCompletedKafkaListenerContainerFactory(
                        DefaultErrorHandler sagaErrorHandler) {
                ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(paymentCompletedConsumerFactory());
                factory.setCommonErrorHandler(sagaErrorHandler);
                factory.getContainerProperties()
                                .setAckMode(ContainerProperties.AckMode.RECORD);

                return factory;
        }

        @Bean
        public ConsumerFactory<String, InventoryReservedEvent> inventoryReservedConsumerFactory() {

                Map<String, Object> props = new HashMap<>();
                props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
                props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

                JsonDeserializer<InventoryReservedEvent> deserializer = new JsonDeserializer<>(
                                InventoryReservedEvent.class);

                deserializer.addTrustedPackages("com.oms.eventcontracts");
                deserializer.setUseTypeHeaders(false);

                return new DefaultKafkaConsumerFactory<>(
                                props,
                                new StringDeserializer(),
                                deserializer);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> inventoryReservedKafkaListenerContainerFactory(
                        DefaultErrorHandler sagaErrorHandler) {
                ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
                factory.setConsumerFactory(inventoryReservedConsumerFactory());
                factory.setCommonErrorHandler(sagaErrorHandler);
                factory.getContainerProperties()
                                .setAckMode(ContainerProperties.AckMode.RECORD);
                return factory;
        }

        @Bean
        public ConsumerFactory<String, InventoryUnavailableEvent>
        inventoryUnavailableConsumerFactory() {

                Map<String, Object> props = new HashMap<>();
                props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
                props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

                JsonDeserializer<InventoryUnavailableEvent> deserializer =
                        new JsonDeserializer<>(InventoryUnavailableEvent.class);

                deserializer.addTrustedPackages("com.oms.eventcontracts");
                deserializer.setUseTypeHeaders(false);

                return new DefaultKafkaConsumerFactory<>(
                        props,
                        new StringDeserializer(),
                        deserializer
                );
        }


        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, InventoryUnavailableEvent>
        inventoryUnavailableKafkaListenerContainerFactory(
                DefaultErrorHandler sagaErrorHandler) {

                ConcurrentKafkaListenerContainerFactory<String, InventoryUnavailableEvent> factory =
                        new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(inventoryUnavailableConsumerFactory());
                factory.setCommonErrorHandler(sagaErrorHandler);
                factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

                return factory;
        }


        @Bean
        public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
                        KafkaTemplate<String, Object> kafkaTemplate) {
                DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                                kafkaTemplate,
                                (record, ex) -> new TopicPartition(
                                                "saga.events.dlq",
                                                record.partition())

                );

                recoverer.setHeadersFunction((record, ex) -> {
                        Headers headers = new RecordHeaders();

                        headers.add(
                                        "dlq-exception-class",
                                        ex.getClass().getName().getBytes(StandardCharsets.UTF_8));
                        headers.add(
                                        "dlq-exception-message",
                                        String.valueOf(ex.getMessage()).getBytes(StandardCharsets.UTF_8));
                        headers.add(
                                        "dlq-original-topic",
                                        record.topic().getBytes(StandardCharsets.UTF_8));
                        headers.add(
                                        "dlq-original-partition",
                                        String.valueOf(record.partition()).getBytes(StandardCharsets.UTF_8));
                        headers.add(
                                        "dlq-original-offset",
                                        String.valueOf(record.offset()).getBytes(StandardCharsets.UTF_8));

                        return headers;
                });

                return recoverer;
        }

}
