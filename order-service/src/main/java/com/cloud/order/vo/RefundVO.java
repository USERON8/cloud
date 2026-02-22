package com.cloud.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Refund view object")
public class RefundVO {

    @Schema(description = "Refund ID")
    private Long id;

    @Schema(description = "Refund number")
    private String refundNo;

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Order number")
    private String orderNo;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Merchant ID")
    private Long merchantId;

    @Schema(description = "Refund type: 1 refund only, 2 return and refund")
    private Integer refundType;

    @Schema(description = "Refund type display name")
    private String refundTypeName;

    @Schema(description = "Refund reason")
    private String refundReason;

    @Schema(description = "Refund description")
    private String refundDescription;

    @Schema(description = "Refund amount")
    private BigDecimal refundAmount;

    @Schema(description = "Refund quantity")
    private Integer refundQuantity;

    @Schema(description = "Refund status")
    private Integer status;

    @Schema(description = "Refund status display name")
    private String statusName;

    @Schema(description = "Audit time")
    private LocalDateTime auditTime;

    @Schema(description = "Audit remark")
    private String auditRemark;

    @Schema(description = "Logistics company")
    private String logisticsCompany;

    @Schema(description = "Logistics number")
    private String logisticsNo;

    @Schema(description = "Refund time")
    private LocalDateTime refundTime;

    @Schema(description = "Refund channel")
    private String refundChannel;

    @Schema(description = "Refund transaction number")
    private String refundTransactionNo;

    @Schema(description = "Created time")
    private LocalDateTime createdAt;

    @Schema(description = "Updated time")
    private LocalDateTime updatedAt;
}
