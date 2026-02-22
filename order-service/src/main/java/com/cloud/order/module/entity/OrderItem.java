package com.cloud.order.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;








@EqualsAndHashCode(callSuper = true)
@TableName(value = "order_item")
@Data
public class OrderItem extends BaseEntity<OrderItem> {
    


    @TableField(value = "order_id")
    private Long orderId;

    


    @TableField(value = "product_id")
    private Long productId;

    



    @TableField(value = "product_snapshot")
    private String productSnapshot;

    


    @TableField(value = "quantity")
    private Integer quantity;

    


    @TableField(value = "price")
    private BigDecimal price;

    


    @TableField(value = "create_by")
    private Long createBy;

    


    @TableField(value = "update_by")
    private Long updateBy;

    
}
