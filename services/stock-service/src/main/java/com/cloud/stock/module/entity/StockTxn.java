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

  @TableField("segment_id")
  private Integer segmentId;

  @TableField("sub_order_no")
  private String subOrderNo;

  @TableField("txn_type")
  private String txnType;

  @TableField("quantity")
  private Integer quantity;

  @TableField("before_available")
  private Integer beforeAvailable;

  @TableField("after_available")
  private Integer afterAvailable;

  @TableField("before_locked")
  private Integer beforeLocked;

  @TableField("after_locked")
  private Integer afterLocked;

  @TableField("before_sold")
  private Integer beforeSold;

  @TableField("after_sold")
  private Integer afterSold;

  @TableField("remark")
  private String remark;
}
