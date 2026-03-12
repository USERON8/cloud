package com.cloud.stock.messaging;

import com.cloud.common.messaging.event.StockFreezeFailedEvent;
import com.cloud.common.messaging.outbox.OutboxEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.cloud.common.messaging.outbox.OutboxProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockOutboxRelay {

    private final OutboxEventService outboxEventService;
    private final OutboxProperties outboxProperties;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    public void dispatch() {
        if (!outboxProperties.isEnabled()) {
            return;
        }
        List<OutboxEvent> events = outboxEventService.fetchDueEvents(outboxProperties.getBatchSize());
        if (events.isEmpty()) {
            return;
        }

        for (OutboxEvent event : events) {
            if (!outboxEventService.markProcessing(event.getId())) {
                continue;
            }
            boolean sent = false;
            try {
                sent = sendEvent(event);
            } catch (Exception ex) {
                log.warn("Outbox dispatch failed: eventId={}, eventType={}", event.getEventId(), event.getEventType(), ex);
            }

            if (sent) {
                outboxEventService.markSent(event.getId());
            } else {
                outboxEventService.markFailed(event, outboxProperties.getMaxRetry(), outboxProperties.getRetryBackoffSeconds());
            }
        }
    }

    private boolean sendEvent(OutboxEvent event) throws Exception {
        String eventType = event.getEventType();
        if (eventType == null || eventType.isBlank()) {
            log.warn("Outbox event type missing: eventId={}", event.getEventId());
            return false;
        }

        return switch (eventType) {
            case "STOCK_FREEZE_FAILED" -> sendStockFreezeFailed(event);
            default -> {
                log.warn("Unknown outbox event type: eventId={}, eventType={}", event.getEventId(), eventType);
                yield false;
            }
        };
    }

    private boolean sendStockFreezeFailed(OutboxEvent event) throws Exception {
        StockFreezeFailedEvent payload = objectMapper.readValue(event.getPayload(), StockFreezeFailedEvent.class);
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_KEYS, payload.getOrderNo());
        headers.put(MessageConst.PROPERTY_TAGS, "STOCK_FREEZE_FAILED");
        headers.put("eventId", payload.getEventId());
        headers.put("eventType", payload.getEventType());

        Message<StockFreezeFailedEvent> message = MessageBuilder.withPayload(payload)
                .copyHeaders(headers)
                .build();
        return streamBridge.send("stockFreezeFailedProducer-out-0", message);
    }
}
