package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;







@EqualsAndHashCode(callSuper = true)
@TableName(value = "stock_out")
@Data
public class StockOut extends BaseEntity<StockOut> {
    


    @TableField(value = "product_id")
    private Long productId;

    


    @TableField(value = "order_id")
    private Long orderId;

    


    @TableField(value = "quantity")
    private Integer quantity;
}

