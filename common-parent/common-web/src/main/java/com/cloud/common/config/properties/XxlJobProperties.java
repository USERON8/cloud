package com.cloud.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {

  private boolean enabled;

  private String adminAddresses;

  private String accessToken;

  private final Executor executor = new Executor();

  @Data
  public static class Executor {
    private String appname;
    private String address;
    private String ip;
    private int port;
    private String logPath = "logs/xxl-job";
    private int logRetentionDays = 30;
  }
}
