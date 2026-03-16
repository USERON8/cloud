package com.cloud.common.domain.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PaymentFlowDTO {

  private Long id;

  private String paymentId;

  private Integer flowType;

  private BigDecimal amount;

  private String traceId;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private Integer deleted;
}
