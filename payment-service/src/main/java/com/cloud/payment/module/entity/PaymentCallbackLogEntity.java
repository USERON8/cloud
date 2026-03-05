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

    @TableField("callback_no")
    private String callbackNo;

    @TableField("callback_status")
    private String callbackStatus;

    @TableField("provider_txn_no")
    private String providerTxnNo;

    @TableField("payload")
    private String payload;

    @TableField("idempotency_key")
    private String idempotencyKey;
}
