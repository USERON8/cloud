package com.cloud.common.domain.vo.payment;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PaymentRefundVO implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private Long id;
  private String refundNo;
  private String paymentNo;
  private String afterSaleNo;
  private BigDecimal refundAmount;
  private String status;
  private String reason;
  private String idempotencyKey;
  private LocalDateTime refundedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
