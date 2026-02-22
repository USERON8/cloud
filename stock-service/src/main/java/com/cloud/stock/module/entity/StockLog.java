package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;







@EqualsAndHashCode(callSuper = true)
@TableName(value = "stock_log")
@Data
public class StockLog extends BaseEntity<StockLog> {
    


    @TableField(value = "product_id")
    private Long productId;

    


    @TableField(value = "product_name")
    private String productName;

    


    @TableField(value = "operation_type")
    private String operationType;

    


    @TableField(value = "quantity_before")
    private Integer quantityBefore;

    


    @TableField(value = "quantity_after")
    private Integer quantityAfter;

    


    @TableField(value = "quantity_change")
    private Integer quantityChange;

    


    @TableField(value = "order_id")
    private Long orderId;

    


    @TableField(value = "order_no")
    private String orderNo;

    


    @TableField(value = "operator_id")
    private Long operatorId;

    


    @TableField(value = "operator_name")
    private String operatorName;

    


    @TableField(value = "remark")
    private String remark;

    


    @TableField(value = "operate_time")
    private LocalDateTime operateTime;

    


    @TableField(value = "ip_address")
    private String ipAddress;
}
