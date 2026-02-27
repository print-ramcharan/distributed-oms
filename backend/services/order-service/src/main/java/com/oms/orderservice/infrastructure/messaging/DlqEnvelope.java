package com.oms.orderservice.infrastructure.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class DlqEnvelope {

    private String sourceService;
    private String originalTopic;
    private String eventType;
    private String aggregateId;
    private Object payload;
    private String exceptionMessage;
    private Instant failedAt;

}
