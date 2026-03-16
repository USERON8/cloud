package com.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("after_sale")
public class AfterSale extends BaseEntity<AfterSale> {

  @TableField("after_sale_no")
  private String afterSaleNo;

  @TableField("main_order_id")
  private Long mainOrderId;

  @TableField("sub_order_id")
  private Long subOrderId;

  @TableField("user_id")
  private Long userId;

  @TableField("merchant_id")
  private Long merchantId;

  @TableField("after_sale_type")
  private String afterSaleType;

  @TableField("status")
  private String status;

  @TableField("reason")
  private String reason;

  @TableField("description")
  private String description;

  @TableField("apply_amount")
  private BigDecimal applyAmount;

  @TableField("approved_amount")
  private BigDecimal approvedAmount;

  @TableField("return_logistics_company")
  private String returnLogisticsCompany;

  @TableField("return_logistics_no")
  private String returnLogisticsNo;

  @TableField("refund_channel")
  private String refundChannel;

  @TableField("refunded_at")
  private LocalDateTime refundedAt;

  @TableField("closed_at")
  private LocalDateTime closedAt;

  @TableField("close_reason")
  private String closeReason;
}
