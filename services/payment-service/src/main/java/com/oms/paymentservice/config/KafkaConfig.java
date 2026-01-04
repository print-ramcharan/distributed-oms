package com.oms.paymentservice.config;

import com.oms.eventcontracts.commands.InitiatePaymentCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.TopicPartition;


import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {


    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {

        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn(
                    "Retry {} for record topic={}, partition={}, offset={}",
                    deliveryAttempt,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    ex
            );
        });

        errorHandler.addNotRetryableExceptions(
                org.springframework.kafka.support.serializer.DeserializationException.class,
                org.apache.kafka.common.errors.SerializationException.class,
                IllegalArgumentException.class
        );

        errorHandler.setCommitRecovered(true);

        return errorHandler;
    }


    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);


        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    @Bean
    public ConsumerFactory<String, InitiatePaymentCommand> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.class);

        props.put("spring.deserializer.key.delegate.class",
                StringDeserializer.class);
        props.put("spring.deserializer.value.delegate.class",
                JsonDeserializer.class);

        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.oms.eventcontracts");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.oms.eventcontracts.commands.InitiatePaymentCommand");


        return new DefaultKafkaConsumerFactory<>(props);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InitiatePaymentCommand>
    kafkaListenerContainerFactory(DefaultErrorHandler kafkaErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, InitiatePaymentCommand> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler);

        return factory;
    }


    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, ex) -> new TopicPartition(
                                record.topic() + ".dlq",
                                record.partition()
                        )
                );

        recoverer.setHeadersFunction((record, ex) -> {
            Headers headers = new RecordHeaders();

            headers.add(
                    "dlq-exception-class",
                    ex.getClass().getName().getBytes(StandardCharsets.UTF_8)
            );
            headers.add(
                    "dlq-exception-message",
                    String.valueOf(ex.getMessage()).getBytes(StandardCharsets.UTF_8)
            );
            headers.add(
                    "dlq-original-topic",
                    record.topic().getBytes(StandardCharsets.UTF_8)
            );
            headers.add(
                    "dlq-original-partition",
                    String.valueOf(record.partition()).getBytes(StandardCharsets.UTF_8)
            );
            headers.add(
                    "dlq-original-offset",
                    String.valueOf(record.offset()).getBytes(StandardCharsets.UTF_8)
            );

            return headers;
        });

        return recoverer;
    }


}
