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
     * 低库存预警阈值
     */
    @TableField(value = "low_stock_threshold")
    private Integer lowStockThreshold;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField(value = "version")
    private Integer version;

    /**
     * 获取可用库存数量（总库存 - 冻结库存）
     *
     * @return 可用库存数量
     */
    public Integer getAvailableQuantity() {
        if (stockQuantity == null || frozenQuantity == null) {
            return null;
        }
        return stockQuantity - frozenQuantity;
    }

    /**
     * 获取预留库存数量（即冻结库存）
     *
     * @return 预留库存数量
     */
    public Integer getReservedQuantity() {
        return frozenQuantity;
    }

    /**
     * 判断是否为低库存
     *
     * @return true-低库存，false-库存充足
     */
    public boolean isLowStock() {
        if (lowStockThreshold == null || lowStockThreshold == 0) {
            return false;  // 未设置阈值，不触发预警
        }
        Integer available = getAvailableQuantity();
        return available != null && available <= lowStockThreshold;
    }
}
