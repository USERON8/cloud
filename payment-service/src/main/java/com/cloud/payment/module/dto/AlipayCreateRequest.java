package com.cloud.payment.module.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Alipay create payment request")
public class AlipayCreateRequest {

    @NotNull(message = "Order id cannot be null")
    @Schema(description = "Order id", example = "1234567890")
    private Long orderId;

    @NotNull(message = "Payment amount cannot be null")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    @Schema(description = "Payment amount", example = "99.99")
    private BigDecimal amount;

    @NotBlank(message = "Payment subject cannot be blank")
    @Schema(description = "Payment subject", example = "iPhone 15 Pro Max")
    private String subject;

    @Schema(description = "Payment body", example = "iPhone 15 Pro Max 256GB")
    private String body;

    @NotNull(message = "User id cannot be null")
    @Schema(description = "User id", example = "1001")
    private Long userId;

    @Schema(description = "Payment timeout in minutes", example = "30")
    private Integer timeoutMinutes = 30;

    @Schema(description = "Alipay product code", example = "FAST_INSTANT_TRADE_PAY")
    private String productCode = "FAST_INSTANT_TRADE_PAY";
}
