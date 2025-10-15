package com.cloud.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款信息VO
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
@Data
@Schema(description = "退款信息")
public class RefundVO {

    @Schema(description = "退款单ID")
    private Long id;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "退款类型：1-仅退款，2-退货退款")
    private Integer refundType;

    @Schema(description = "退款类型名称")
    private String refundTypeName;

    @Schema(description = "退款原因")
    private String refundReason;

    @Schema(description = "详细说明")
    private String refundDescription;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "退货数量")
    private Integer refundQuantity;

    @Schema(description = "退款状态：0-待审核，1-审核通过，2-审核拒绝，3-退货中，4-已收货，5-退款中，6-已完成，7-已取消，8-已关闭")
    private Integer status;

    @Schema(description = "退款状态名称")
    private String statusName;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "物流公司")
    private String logisticsCompany;

    @Schema(description = "物流单号")
    private String logisticsNo;

    @Schema(description = "退款时间")
    private LocalDateTime refundTime;

    @Schema(description = "退款渠道")
    private String refundChannel;

    @Schema(description = "退款交易号")
    private String refundTransactionNo;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
