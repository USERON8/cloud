package com.cloud.common.messaging.config;

import com.cloud.common.config.properties.MessageProperties;
import com.cloud.common.messaging.deadletter.DeadLetterOpsService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class DeadLetterMonitor {

  private final DeadLetterOpsService deadLetterOpsService;
  private final MessageProperties messageProperties;
  private final AtomicLong pendingDeadLetters = new AtomicLong();

  public DeadLetterMonitor(
      DeadLetterOpsService deadLetterOpsService,
      MessageProperties messageProperties,
      MeterRegistry meterRegistry) {
    this.deadLetterOpsService = deadLetterOpsService;
    this.messageProperties = messageProperties;
    Gauge.builder("mq.dead_letter.pending", pendingDeadLetters, AtomicLong::get)
        .register(meterRegistry);
  }

  @Scheduled(fixedDelayString = "${app.message.monitor.lag-scan-interval-ms:60000}")
  public void refreshPendingCount() {
    long count = deadLetterOpsService.countPending();
    pendingDeadLetters.set(count);
    long threshold = Math.max(0L, messageProperties.getMonitor().getDeadLetterAlertThreshold());
    if (threshold > 0 && count >= threshold) {
      log.warn(
          "Dead letter backlog exceeded threshold: pendingCount={}, threshold={}",
          count,
          threshold);
    }
  }
}
