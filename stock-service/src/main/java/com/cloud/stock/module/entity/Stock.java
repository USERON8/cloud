package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;







@EqualsAndHashCode(callSuper = true)
@TableName(value = "stock")
@Data
public class Stock extends BaseEntity<Stock> {
    


    @TableField(value = "product_id")
    private Long productId;

    


    @TableField(value = "product_name")
    private String productName;

    


    @TableField(value = "stock_quantity")
    private Integer stockQuantity;

    


    @TableField(value = "frozen_quantity")
    private Integer frozenQuantity;

    


    @TableField(value = "stock_status")
    private Integer stockStatus;

    


    @TableField(value = "low_stock_threshold")
    private Integer lowStockThreshold;

    


    @Version
    @TableField(value = "version")
    private Integer version;

    




    public Integer getAvailableQuantity() {
        if (stockQuantity == null || frozenQuantity == null) {
            return null;
        }
        return stockQuantity - frozenQuantity;
    }

    




    public Integer getReservedQuantity() {
        return frozenQuantity;
    }

    




    public boolean isLowStock() {
        if (lowStockThreshold == null || lowStockThreshold == 0) {
            return false;  
        }
        Integer available = getAvailableQuantity();
        return available != null && available <= lowStockThreshold;
    }
}
