package com.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;



@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_tcc_log")
public class OrderTccLog extends BaseEntity<OrderTccLog> {

    @TableField("business_key")
    private String businessKey;

    @TableField("main_order_id")
    private Long mainOrderId;

    @TableField("main_order_no")
    private String mainOrderNo;

    @TableField("status")
    private String status;
}
