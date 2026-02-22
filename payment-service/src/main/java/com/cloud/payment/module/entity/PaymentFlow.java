package com.cloud.payment.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;






@EqualsAndHashCode(callSuper = true)
@TableName(value = "payment_flow")
@Data
public class PaymentFlow extends BaseEntity<PaymentFlow> {
    


    @TableField(value = "payment_id")
    private Long paymentId;

    


    @TableField(value = "flow_type")
    private Integer flowType;

    


    @TableField(value = "amount")
    private BigDecimal amount;

    


    @TableField(value = "trace_id")
    private String traceId;
}
