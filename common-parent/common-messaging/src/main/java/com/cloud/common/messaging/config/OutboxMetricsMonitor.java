package com.cloud.common.messaging.config;

import com.cloud.common.messaging.outbox.OutboxEventMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;

public class OutboxMetricsMonitor {

  private final OutboxEventMapper outboxEventMapper;
  private final AtomicLong pendingCount = new AtomicLong();
  private final AtomicLong oldestPendingAgeSeconds = new AtomicLong();

  public OutboxMetricsMonitor(OutboxEventMapper outboxEventMapper, MeterRegistry meterRegistry) {
    this.outboxEventMapper = outboxEventMapper;
    Gauge.builder("outbox.pending", pendingCount, AtomicLong::get).register(meterRegistry);
    Gauge.builder("outbox.oldest.pending.age.seconds", oldestPendingAgeSeconds, AtomicLong::get)
        .register(meterRegistry);
  }

  @Scheduled(fixedDelayString = "${app.outbox.monitor.scan-interval-ms:60000}")
  public void refresh() {
    pendingCount.set(outboxEventMapper.countPending());
    oldestPendingAgeSeconds.set(Math.max(0L, outboxEventMapper.oldestPendingAgeSeconds()));
  }
}
