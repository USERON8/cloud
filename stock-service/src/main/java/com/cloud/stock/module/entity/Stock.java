package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存表
 *
 * @author what's up
 * @TableName stock
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "stock")
@Data
public class Stock extends BaseEntity<Stock> {
    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * 商品名称
     */
    @TableField(value = "product_name")
    private String productName;

    /**
     * 总库存量
     */
    @TableField(value = "stock_quantity")
    private Integer stockQuantity;

    /**
     * 冻结库存量
     */
    @TableField(value = "frozen_quantity")
    private Integer frozenQuantity;

    /**
     * 库存状态：1-正常，2-缺货，3-下架
     */
    @TableField(value = "stock_status")
    private Integer stockStatus;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField(value = "version")
    private Integer version;
}
