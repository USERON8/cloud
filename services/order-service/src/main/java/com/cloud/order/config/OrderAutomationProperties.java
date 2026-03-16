package com.cloud.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "order.automation")
public class OrderAutomationProperties {

  private final AutoConfirm autoConfirm = new AutoConfirm();
  private final AfterSale afterSale = new AfterSale();

  @Data
  public static class AutoConfirm {
    private int batchSize = 100;
    private int afterHours = 240;
  }

  @Data
  public static class AfterSale {
    private int batchSize = 100;
    private int auditTimeoutHours = 48;
  }
}
