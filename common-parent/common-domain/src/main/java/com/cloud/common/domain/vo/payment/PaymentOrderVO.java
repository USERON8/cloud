package com.cloud.common.domain.vo.payment;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PaymentOrderVO implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private Long id;
  private String paymentNo;
  private String mainOrderNo;
  private String subOrderNo;
  private Long userId;
  private BigDecimal amount;
  private String channel;
  private String status;
  private String providerTxnNo;
  private String idempotencyKey;
  private LocalDateTime paidAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
