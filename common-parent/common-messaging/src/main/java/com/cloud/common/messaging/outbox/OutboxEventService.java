package com.cloud.common.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {

    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_DEAD = "DEAD";

    private final OutboxEventMapper outboxEventMapper;

    @Transactional(rollbackFor = Exception.class)
    public OutboxEvent enqueue(String aggregateType,
                               String aggregateId,
                               String eventType,
                               String payload,
                               String eventId) {
        String safeAggregateType = requireText(aggregateType, "aggregateType");
        String safeEventType = requireText(eventType, "eventType");
        String safePayload = requireText(payload, "payload");
        String safeEventId = StringUtils.hasText(eventId) ? eventId.trim() : UUID.randomUUID().toString();
        String safeAggregateId = StringUtils.hasText(aggregateId) ? aggregateId.trim() : safeEventId;

        OutboxEvent event = new OutboxEvent();
        event.setEventId(safeEventId);
        event.setAggregateType(safeAggregateType);
        event.setAggregateId(safeAggregateId);
        event.setEventType(safeEventType);
        event.setPayload(safePayload);
        event.setStatus(STATUS_NEW);
        event.setRetryCount(0);

        outboxEventMapper.insert(event);
        return event;
    }

    public List<OutboxEvent> fetchDueEvents(int limit) {
        int safeLimit = Math.max(1, limit);
        return outboxEventMapper.selectDueEvents(safeLimit);
    }

    public boolean markProcessing(Long id) {
        if (id == null) {
            return false;
        }
        return outboxEventMapper.markProcessing(id) > 0;
    }

    public void markSent(Long id) {
        if (id == null) {
            return;
        }
        outboxEventMapper.markSent(id);
    }

    public void markFailed(OutboxEvent event, int maxRetry, int backoffSeconds) {
        if (event == null || event.getId() == null) {
            return;
        }
        int retry = event.getRetryCount() == null ? 0 : event.getRetryCount();
        int nextRetry = retry + 1;
        int safeMaxRetry = Math.max(1, maxRetry);
        int safeBackoff = Math.max(1, backoffSeconds);

        String status = nextRetry >= safeMaxRetry ? STATUS_DEAD : STATUS_FAILED;
        LocalDateTime nextRetryAt = STATUS_DEAD.equals(status)
                ? null
                : LocalDateTime.now().plusSeconds((long) safeBackoff * nextRetry);

        outboxEventMapper.updateStatus(event.getId(), status, nextRetry, nextRetryAt);
        if (STATUS_DEAD.equals(status)) {
            log.warn("Outbox event reached max retry, marked dead: eventId={}, eventType={}",
                    event.getEventId(), event.getEventType());
        }
    }

    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
