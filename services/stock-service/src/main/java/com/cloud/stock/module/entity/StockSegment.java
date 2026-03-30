package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("stock_segment")
public class StockSegment extends BaseEntity<StockSegment> {

  @TableField("sku_id")
  private Long skuId;

  @TableField("segment_id")
  private Integer segmentId;

  @TableField("available_qty")
  private Integer availableQty;

  @TableField("locked_qty")
  private Integer lockedQty;

  @TableField("sold_qty")
  private Integer soldQty;

  @TableField("alert_threshold")
  private Integer alertThreshold;

  @TableField("status")
  private Integer status;
}
