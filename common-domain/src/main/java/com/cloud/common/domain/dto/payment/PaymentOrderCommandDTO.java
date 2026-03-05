package com.cloud.common.domain.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PaymentOrderCommandDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String paymentNo;

    @NotBlank
    private String mainOrderNo;

    @NotBlank
    private String subOrderNo;

    @NotNull
    private Long userId;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal amount;

    @NotBlank
    private String channel;

    @NotBlank
    private String idempotencyKey;
}
