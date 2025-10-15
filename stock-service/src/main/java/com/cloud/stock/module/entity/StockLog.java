package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 库存操作日志表
 *
 * @author what's up
 * @TableName stock_log
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "stock_log")
@Data
public class StockLog extends BaseEntity<StockLog> {
    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * 商品名称
     */
    @TableField(value = "product_name")
    private String productName;

    /**
     * 操作类型：IN-入库，OUT-出库，RESERVE-预留，RELEASE-释放，ADJUST-调整，COUNT-盘点
     */
    @TableField(value = "operation_type")
    private String operationType;

    /**
     * 操作前库存数量
     */
    @TableField(value = "quantity_before")
    private Integer quantityBefore;

    /**
     * 操作后库存数量
     */
    @TableField(value = "quantity_after")
    private Integer quantityAfter;

    /**
     * 操作数量（变化量）
     */
    @TableField(value = "quantity_change")
    private Integer quantityChange;

    /**
     * 关联订单ID
     */
    @TableField(value = "order_id")
    private Long orderId;

    /**
     * 关联订单号
     */
    @TableField(value = "order_no")
    private String orderNo;

    /**
     * 操作人ID
     */
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 操作人名称
     */
    @TableField(value = "operator_name")
    private String operatorName;

    /**
     * 操作备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 操作时间
     */
    @TableField(value = "operate_time")
    private LocalDateTime operateTime;

    /**
     * 操作IP地址
     */
    @TableField(value = "ip_address")
    private String ipAddress;
}
