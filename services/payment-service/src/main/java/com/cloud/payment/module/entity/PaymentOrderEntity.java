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
@TableName("payment_order")
public class PaymentOrderEntity extends BaseEntity<PaymentOrderEntity> {

  @TableField("payment_no")
  private String paymentNo;

  @TableField("main_order_no")
  private String mainOrderNo;

  @TableField("sub_order_no")
  private String subOrderNo;

  @TableField("user_id")
  private Long userId;

  @TableField("amount")
  private BigDecimal amount;

  @TableField("provider")
  private String provider;

  @TableField("provider_app_id")
  private String providerAppId;

  @TableField("provider_merchant_id")
  private String providerMerchantId;

  @TableField("biz_type")
  private String bizType;

  @TableField("biz_order_key")
  private String bizOrderKey;

  @TableField("channel")
  private String channel;

  @TableField("status")
  private String status;

  @TableField("provider_txn_no")
  private String providerTxnNo;

  @TableField("idempotency_key")
  private String idempotencyKey;

  @TableField("paid_at")
  private LocalDateTime paidAt;

  @TableField("poll_count")
  private Integer pollCount;

  @TableField("next_poll_at")
  private LocalDateTime nextPollAt;

  @TableField("last_polled_at")
  private LocalDateTime lastPolledAt;

  @TableField("last_poll_error")
  private String lastPollError;
}
