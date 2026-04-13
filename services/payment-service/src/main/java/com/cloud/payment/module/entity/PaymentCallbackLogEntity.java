package com.cloud.payment.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_callback_log")
public class PaymentCallbackLogEntity extends BaseEntity<PaymentCallbackLogEntity> {

  @TableField("payment_no")
  private String paymentNo;

  @TableField("provider")
  private String provider;

  @TableField("callback_no")
  private String callbackNo;

  @TableField("callback_status")
  private String callbackStatus;

  @TableField("provider_event_type")
  private String providerEventType;

  @TableField("provider_txn_no")
  private String providerTxnNo;

  @TableField("verified_app_id")
  private String verifiedAppId;

  @TableField("verified_seller_id")
  private String verifiedSellerId;

  @TableField("payload")
  private String payload;

  @TableField("raw_payload_hash")
  private String rawPayloadHash;

  @TableField("idempotency_key")
  private String idempotencyKey;
}
