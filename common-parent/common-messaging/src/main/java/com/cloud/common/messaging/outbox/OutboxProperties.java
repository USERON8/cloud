package com.cloud.common.messaging.outbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {

  private boolean enabled = true;

  private int batchSize = 100;

  private long pollIntervalMs = 2000L;

  private int maxRetry = 8;

  private int retryBackoffSeconds = 30;
}
