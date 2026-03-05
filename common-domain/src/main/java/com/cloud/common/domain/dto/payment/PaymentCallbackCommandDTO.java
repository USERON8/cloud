package com.cloud.common.domain.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PaymentCallbackCommandDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String paymentNo;

    @NotBlank
    private String callbackNo;

    @NotBlank
    private String callbackStatus;

    private String providerTxnNo;

    @NotBlank
    private String idempotencyKey;

    private String payload;
}
