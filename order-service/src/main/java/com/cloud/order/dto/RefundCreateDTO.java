package com.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建退款申请DTO
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
@Data
@Schema(description = "创建退款申请请求")
public class RefundCreateDTO {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID", example = "1")
    private Long orderId;

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号", example = "ORD1234567890")
    private String orderNo;

    @NotNull(message = "退款类型不能为空")
    @Min(value = 1, message = "退款类型必须为1或2")
    @Max(value = 2, message = "退款类型必须为1或2")
    @Schema(description = "退款类型：1-仅退款，2-退货退款", example = "1")
    private Integer refundType;

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 255, message = "退款原因不能超过255个字符")
    @Schema(description = "退款原因", example = "商品质量问题")
    private String refundReason;

    @Size(max = 1000, message = "详细说明不能超过1000个字符")
    @Schema(description = "详细说明", example = "收到商品有破损")
    private String refundDescription;

    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    @Schema(description = "退款金额", example = "99.99")
    private BigDecimal refundAmount;

    @Min(value = 1, message = "退货数量必须大于0")
    @Schema(description = "退货数量", example = "1")
    private Integer refundQuantity;
}
