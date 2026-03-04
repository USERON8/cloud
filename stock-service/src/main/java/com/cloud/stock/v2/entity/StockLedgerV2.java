package com.cloud.stock.v2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock_ledger")
public class StockLedgerV2 extends BaseEntity<StockLedgerV2> {

    @TableField("sku_id")
    private Long skuId;

    @TableField("sku_code")
    private String skuCode;

    @TableField("sku_name")
    private String skuName;

    @TableField("on_hand_qty")
    private Integer onHandQty;

    @TableField("reserved_qty")
    private Integer reservedQty;

    @TableField("salable_qty")
    private Integer salableQty;

    @TableField("warning_threshold")
    private Integer warningThreshold;

    @TableField("stock_status")
    private String stockStatus;
}

