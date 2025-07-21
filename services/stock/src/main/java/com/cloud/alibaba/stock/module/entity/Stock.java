package com.cloud.alibaba.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_stock")
public class Stock extends BaseEntity {

    /**
     * 商品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 商品名称
     */
    @TableField("product_name")
    private String productName;

    /**
     * 总库存数量
     */
    @TableField("stock_count")
    private Integer stockCount;

    /**
     * 冻结库存数量
     */
    @TableField("frozen_count")
    private Integer frozenCount;

    /**
     * 可用库存数量
     */
    @TableField("available_count")
    private Integer availableCount;

    /**
     * 版本号（乐观锁）
     */
    @TableField("version")
    private Integer version;
}
