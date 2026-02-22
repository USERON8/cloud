package com.cloud.payment.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;






@EqualsAndHashCode(callSuper = true)
@TableName(value = "payment")
@Data
public class Payment extends BaseEntity<Payment> {
    


    @TableField(value = "order_id")
    private Long orderId;

    


    @TableField(value = "user_id")
    private Long userId;

    


    @TableField(value = "amount")
    private BigDecimal amount;

    


    @TableField(value = "status")
    private Integer status;

    


    @TableField(value = "channel")
    private Integer channel;

    


    @TableField(value = "transaction_id")
    private String transactionId;

    



    @TableField(value = "encrypted_transaction_id", exist = false)
    private String encryptedTransactionId;

    


    @TableField(value = "trace_id")
    private String traceId;
}
