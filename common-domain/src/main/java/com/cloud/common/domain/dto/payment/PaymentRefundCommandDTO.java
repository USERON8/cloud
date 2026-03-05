package com.cloud.common.domain.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PaymentRefundCommandDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String refundNo;

    @NotBlank
    private String paymentNo;

    @NotBlank
    private String afterSaleNo;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal refundAmount;

    @NotBlank
    private String reason;

    @NotBlank
    private String idempotencyKey;
}
