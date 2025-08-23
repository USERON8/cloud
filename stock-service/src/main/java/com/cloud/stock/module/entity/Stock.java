package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存主表（支持高并发）
 *
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
     * 可用库存量（虚拟字段，计算得出）
     */
    @TableField(exist = false)
    private Integer availableQuantity;

    /**
     * 版本号（用于乐观锁）
     */
    @TableField(value = "version")
    private Long version;

}