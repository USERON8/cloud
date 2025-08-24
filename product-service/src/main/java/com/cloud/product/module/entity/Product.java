package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 商品表
 *
 * @TableName products
 */
@EqualsAndHashCode(callSuper = true)
@TableName("products")
@Data
public class Product extends BaseEntity<Product> {
    /**
     * 店铺ID（引用merchant_db.merchant_shop表的shop_id）
     * 对应数据库字段: shop_id
     */
    @TableField("shop_id")
    private Long shopId;

    /**
     * 商品名称
     * 对应数据库字段: name
     */
    @TableField("name")
    private String name;

    /**
     * 售价
     * 对应数据库字段: price
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 库存数量
     * 对应数据库字段: stock_quantity
     */
    @TableField("stock_quantity")
    private Integer stockQuantity;

    /**
     * 分类ID
     * 对应数据库字段: category_id
     */
    @TableField("category_id")
    private Integer categoryId;

    /**
     * 商品状态
     * 对应数据库字段: status
     * @see ProductStatusEnum
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 商品状态枚举
     * 0-下架，1-上架
     */
    @Getter
    public enum ProductStatusEnum {
        OFF_SHELF(0, "下架"),
        ON_SHELF(1, "上架");
        
        private final int code;
        private final String description;
        
        ProductStatusEnum(int code, String description) {
            this.code = code;
            this.description = description;
        }

    }
}