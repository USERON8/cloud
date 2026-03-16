package com.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cart_item")
public class CartItem extends BaseEntity<CartItem> {

  @TableField("cart_id")
  private Long cartId;

  @TableField("user_id")
  private Long userId;

  @TableField("spu_id")
  private Long spuId;

  @TableField("sku_id")
  private Long skuId;

  @TableField("sku_name")
  private String skuName;

  @TableField("quantity")
  private Integer quantity;

  @TableField("unit_price")
  private BigDecimal unitPrice;

  @TableField("selected")
  private Integer selected;

  @TableField("checked_out")
  private Integer checkedOut;
}
