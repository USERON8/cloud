package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("stock_ledger")
public class StockLedger extends BaseEntity<StockLedger> {

    @TableField("sku_id")
    private Long skuId;

    @TableField("on_hand_qty")
    private Integer onHandQty;

    @TableField("reserved_qty")
    private Integer reservedQty;

    @TableField("salable_qty")
    private Integer salableQty;

    @TableField("alert_threshold")
    private Integer alertThreshold;

    @TableField("status")
    private Integer status;
}
