package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("stock_txn")
public class StockTxn extends BaseEntity<StockTxn> {

  @TableField("sku_id")
  private Long skuId;

  @TableField("sub_order_no")
  private String subOrderNo;

  @TableField("txn_type")
  private String txnType;

  @TableField("quantity")
  private Integer quantity;

  @TableField("before_on_hand")
  private Integer beforeOnHand;

  @TableField("after_on_hand")
  private Integer afterOnHand;

  @TableField("before_reserved")
  private Integer beforeReserved;

  @TableField("after_reserved")
  private Integer afterReserved;

  @TableField("before_salable")
  private Integer beforeSalable;

  @TableField("after_salable")
  private Integer afterSalable;

  @TableField("remark")
  private String remark;
}
