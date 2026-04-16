package com.cloud.common.messaging.outbox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OutboxGovernanceService {

  private final OutboxEventMapper outboxEventMapper;

  public OutboxGovernanceService(OutboxEventMapper outboxEventMapper) {
    this.outboxEventMapper = outboxEventMapper;
  }

  public Map<String, Object> getStats() {
    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("newCount", outboxEventMapper.countByStatus(OutboxEventService.STATUS_NEW));
    stats.put("failedCount", outboxEventMapper.countByStatus(OutboxEventService.STATUS_FAILED));
    stats.put(
        "processingCount", outboxEventMapper.countByStatus(OutboxEventService.STATUS_PROCESSING));
    stats.put("deadCount", outboxEventMapper.countByStatus(OutboxEventService.STATUS_DEAD));
    stats.put("sentCount", outboxEventMapper.countByStatus(OutboxEventService.STATUS_SENT));
    stats.put("pendingCount", outboxEventMapper.countPending());
    stats.put("oldestPendingAgeSeconds", Math.max(0L, outboxEventMapper.oldestPendingAgeSeconds()));
    return stats;
  }

  public List<OutboxEvent> listPending(int limit) {
    return outboxEventMapper.selectByStatuses(
        List.of(
            OutboxEventService.STATUS_NEW,
            OutboxEventService.STATUS_FAILED,
            OutboxEventService.STATUS_PROCESSING),
        sanitizeLimit(limit));
  }

  public List<OutboxEvent> listDead(int limit) {
    return outboxEventMapper.selectByStatuses(
        List.of(OutboxEventService.STATUS_DEAD), sanitizeLimit(limit));
  }

  public boolean requeue(Long id) {
    if (id == null || id <= 0) {
      return false;
    }
    return outboxEventMapper.requeue(id) > 0;
  }

  private int sanitizeLimit(int limit) {
    return Math.max(1, Math.min(limit, 100));
  }
}
