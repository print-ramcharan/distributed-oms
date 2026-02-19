package com.oms.fulfillmentservice.messaging;

import com.oms.eventcontracts.events.InventoryReservedEvent;
import com.oms.fulfillmentservice.domain.FulfillmentTask;
import com.oms.fulfillmentservice.domain.FulfillmentTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryReservedConsumerTest {

    @Mock
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private InventoryReservedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new InventoryReservedConsumer(fulfillmentTaskRepository, kafkaTemplate);
    }

    @Test
    void shouldCreateAndDispatchTaskWhenEventReceived() {
        
        String orderId = UUID.randomUUID().toString();
        InventoryReservedEvent event = new InventoryReservedEvent(orderId, Instant.now());

        when(fulfillmentTaskRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        
        when(fulfillmentTaskRepository.save(any(FulfillmentTask.class))).thenAnswer(invocation -> {
            FulfillmentTask t = invocation.getArgument(0);
            if (t.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            }
            return t;
        });

        
        consumer.handle(event);

        
        
        ArgumentCaptor<FulfillmentTask> taskCaptor = ArgumentCaptor.forClass(FulfillmentTask.class);
        verify(fulfillmentTaskRepository, times(2)).save(taskCaptor.capture());

        FulfillmentTask savedTask = taskCaptor.getAllValues().get(1); 
        assertThat(savedTask.getOrderId()).isEqualTo(orderId);
        
        
        

        
        verify(kafkaTemplate).send(eq("fulfillment.initiated"), eq(orderId), any(FulfillmentInitiatedEvent.class));
    }

    @Test
    void shouldIgnoreDuplicateEventIfTaskExists() {
        
        String orderId = UUID.randomUUID().toString();
        InventoryReservedEvent event = new InventoryReservedEvent(orderId, Instant.now());

        when(fulfillmentTaskRepository.findByOrderId(orderId)).thenReturn(Optional.of(FulfillmentTask.create(orderId)));

        
        consumer.handle(event);

        
        verify(fulfillmentTaskRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
