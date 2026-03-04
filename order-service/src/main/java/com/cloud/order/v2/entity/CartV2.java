package com.cloud.order.v2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cart")
public class CartV2 extends BaseEntity<CartV2> {

    @TableField("cart_no")
    private String cartNo;

    @TableField("user_id")
    private Long userId;

    @TableField("cart_status")
    private String cartStatus;

    @TableField("selected_count")
    private Integer selectedCount;

    @TableField("total_amount")
    private BigDecimal totalAmount;
}

