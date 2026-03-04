package com.cloud.stock.v2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock_txn")
public class StockTxnV2 extends BaseEntity<StockTxnV2> {

    @TableField("txn_no")
    private String txnNo;

    @TableField("sku_id")
    private Long skuId;

    @TableField("main_order_no")
    private String mainOrderNo;

    @TableField("sub_order_no")
    private String subOrderNo;

    @TableField("txn_type")
    private String txnType;

    @TableField("qty")
    private Integer qty;

    @TableField("before_on_hand")
    private Integer beforeOnHand;

    @TableField("before_reserved")
    private Integer beforeReserved;

    @TableField("before_salable")
    private Integer beforeSalable;

    @TableField("after_on_hand")
    private Integer afterOnHand;

    @TableField("after_reserved")
    private Integer afterReserved;

    @TableField("after_salable")
    private Integer afterSalable;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("remark")
    private String remark;
}

