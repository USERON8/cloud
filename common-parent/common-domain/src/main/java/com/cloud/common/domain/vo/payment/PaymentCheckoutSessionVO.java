package com.cloud.common.domain.vo.payment;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

@Data
public class PaymentCheckoutSessionVO implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private String paymentNo;
  private String checkoutPath;
  private Long expiresInSeconds;
}
