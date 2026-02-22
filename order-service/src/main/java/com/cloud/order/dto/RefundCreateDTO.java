package com.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Refund create request")
public class RefundCreateDTO {

    @NotNull(message = "orderId is required")
    @Schema(description = "Order ID", example = "1")
    private Long orderId;

    @NotBlank(message = "orderNo is required")
    @Schema(description = "Order number", example = "ORD1234567890")
    private String orderNo;

    @NotNull(message = "refundType is required")
    @Min(value = 1, message = "refundType must be 1 or 2")
    @Max(value = 2, message = "refundType must be 1 or 2")
    @Schema(description = "Refund type: 1 refund only, 2 return and refund", example = "1")
    private Integer refundType;

    @NotBlank(message = "refundReason is required")
    @Size(max = 255, message = "refundReason length cannot exceed 255")
    @Schema(description = "Refund reason", example = "Product damaged")
    private String refundReason;

    @Size(max = 1000, message = "refundDescription length cannot exceed 1000")
    @Schema(description = "Refund description", example = "Package was damaged during shipment")
    private String refundDescription;

    @NotNull(message = "refundAmount is required")
    @DecimalMin(value = "0.01", message = "refundAmount must be greater than 0")
    @Schema(description = "Refund amount", example = "99.99")
    private BigDecimal refundAmount;

    @Min(value = 1, message = "refundQuantity must be greater than 0")
    @Schema(description = "Refund quantity", example = "1")
    private Integer refundQuantity;
}