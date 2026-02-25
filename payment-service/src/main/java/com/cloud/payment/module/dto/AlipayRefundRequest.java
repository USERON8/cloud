package com.cloud.payment.module.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Alipay refund request")
public class AlipayRefundRequest {

    @NotBlank(message = "Out trade number cannot be blank")
    @Schema(description = "Merchant out trade number", example = "PAY_20260225153000_100001")
    private String outTradeNo;

    @NotNull(message = "Refund amount cannot be null")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    @Schema(description = "Refund amount", example = "10.00")
    private BigDecimal refundAmount;

    @NotBlank(message = "Refund reason cannot be blank")
    @Schema(description = "Refund reason", example = "User requested refund")
    private String refundReason;
}
