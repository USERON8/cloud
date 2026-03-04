package com.cloud.order.v2.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateMainOrderRequest {
    @NotNull
    private Long userId;
    @DecimalMin("0.01")
    private BigDecimal totalAmount;
    @DecimalMin("0.01")
    private BigDecimal payableAmount;
    private String remark;
    private String idempotencyKey;
}

