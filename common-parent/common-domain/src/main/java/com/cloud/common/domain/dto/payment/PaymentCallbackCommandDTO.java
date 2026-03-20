package com.cloud.common.domain.dto.payment;

import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class PaymentCallbackCommandDTO implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  @NotBlank private String paymentNo;

  @NotBlank private String callbackNo;

  @NotBlank private String callbackStatus;

  private String providerTxnNo;

  @NotBlank private String idempotencyKey;

  private BigDecimal amount;

  private String payload;
}
