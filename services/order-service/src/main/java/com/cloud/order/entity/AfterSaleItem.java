package com.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("after_sale_item")
public class AfterSaleItem extends BaseEntity<AfterSaleItem> {

  @TableField("after_sale_id")
  private Long afterSaleId;

  @TableField("order_item_id")
  private Long orderItemId;

  @TableField("sku_id")
  private Long skuId;

  @TableField("quantity")
  private Integer quantity;

  @TableField("apply_amount")
  private BigDecimal applyAmount;

  @TableField("approved_amount")
  private BigDecimal approvedAmount;
}
