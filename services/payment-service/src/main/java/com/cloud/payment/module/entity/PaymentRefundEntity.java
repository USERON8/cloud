package com.cloud.payment.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_refund")
public class PaymentRefundEntity extends BaseEntity<PaymentRefundEntity> {

  @TableField("refund_no")
  private String refundNo;

  @TableField("payment_no")
  private String paymentNo;

  @TableField("provider")
  private String provider;

  @TableField("provider_app_id")
  private String providerAppId;

  @TableField("provider_merchant_id")
  private String providerMerchantId;

  @TableField("after_sale_no")
  private String afterSaleNo;

  @TableField("refund_amount")
  private BigDecimal refundAmount;

  @TableField("status")
  private String status;

  @TableField("reason")
  private String reason;

  @TableField("idempotency_key")
  private String idempotencyKey;

  @TableField("refunded_at")
  private LocalDateTime refundedAt;

  @TableField("retry_count")
  private Integer retryCount;

  @TableField("next_retry_at")
  private LocalDateTime nextRetryAt;

  @TableField("last_retry_at")
  private LocalDateTime lastRetryAt;

  @TableField("last_error")
  private String lastError;
}
