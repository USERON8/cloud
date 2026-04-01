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

  @TableField("main_order_no")
  private String mainOrderNo;

  @TableField("sub_order_no")
  private String subOrderNo;

  @TableField("sku_id")
  private Long skuId;

  @TableField("segment_id")
  private Integer segmentId;

  @TableField("quantity")
  private Integer quantity;

  @TableField("status")
  private String status;

  @TableField("idempotency_key")
  private String idempotencyKey;
}
