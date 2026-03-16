package com.cloud.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payment.compensation")
public class PaymentCompensationProperties {

  private final OrderQuery orderQuery = new OrderQuery();
  private final RefundRetry refundRetry = new RefundRetry();

  @Data
  public static class OrderQuery {
    private int batchSize = 50;
    private int initialDelaySeconds = 60;
    private int intervalSeconds = 120;
    private int maxAttempts = 10;
  }

  @Data
  public static class RefundRetry {
    private int batchSize = 50;
    private int intervalSeconds = 300;
    private int maxAttempts = 6;
  }
}
