package com.cloud.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.message")
public class MessageProperties {

  private boolean enabled = true;

  private int sendRetryTimes = 3;

  private long sendTimeout = 3000;

  private boolean traceEnabled = true;

  private boolean idempotentEnabled = true;

  private long idempotentExpireSeconds = 86400;

  private HeaderConfig header = new HeaderConfig();

  private LogConfig log = new LogConfig();

  private MonitorConfig monitor = new MonitorConfig();

  @Data
  public static class HeaderConfig {

    private boolean autoTraceId = true;

    private boolean autoTimestamp = true;

    private boolean autoServiceName = true;

    private String customPrefix = "";
  }

  @Data
  public static class LogConfig {

    private boolean verbose = true;

    private boolean logPayload = false;

    private boolean logHeaders = true;

    private int payloadMaxLength = 1000;
  }

  @Data
  public static class MonitorConfig {

    private boolean enabled = true;

    private long lagScanIntervalMs = 60000;

    private long lagAlertThreshold = 1000;

    private long deadLetterAlertThreshold = 10;

    private int deadLetterQueryLimit = 100;

    private boolean adminEndpointEnabled = true;
  }
}
