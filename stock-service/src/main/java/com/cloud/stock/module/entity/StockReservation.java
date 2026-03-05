package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("stock_reservation")
public class StockReservation extends BaseEntity<StockReservation> {

    @TableField("sub_order_no")
    private String subOrderNo;

    @TableField("sku_id")
    private Long skuId;

    @TableField("reserved_qty")
    private Integer reservedQty;

    @TableField("status")
    private String status;

    @TableField("idempotency_key")
    private String idempotencyKey;
}
