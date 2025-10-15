package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品SKU表
 * SKU (Stock Keeping Unit) - 库存量单位，商品的具体规格组合
 *
 * @author what's up
 * @TableName product_sku
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_sku")
@Data
public class ProductSku extends BaseEntity<ProductSku> {
    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * SKU编码
     */
    @TableField(value = "sku_code")
    private String skuCode;

    /**
     * SKU名称
     */
    @TableField(value = "sku_name")
    private String skuName;

    /**
     * 规格值组合 (JSON格式: [{"spec_name":"颜色","spec_value":"红色"},{"spec_name":"尺寸","spec_value":"XL"}])
     */
    @TableField(value = "spec_values")
    private String specValues;

    /**
     * SKU价格
     */
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * SKU原价
     */
    @TableField(value = "original_price")
    private BigDecimal originalPrice;

    /**
     * SKU成本价
     */
    @TableField(value = "cost_price")
    private BigDecimal costPrice;

    /**
     * 库存数量
     */
    @TableField(value = "stock_quantity")
    private Integer stockQuantity;

    /**
     * 已售数量
     */
    @TableField(value = "sales_quantity")
    private Integer salesQuantity;

    /**
     * SKU图片URL
     */
    @TableField(value = "image_url")
    private String imageUrl;

    /**
     * 重量(克)
     */
    @TableField(value = "weight")
    private Integer weight;

    /**
     * 体积(立方厘米)
     */
    @TableField(value = "volume")
    private Integer volume;

    /**
     * 条形码
     */
    @TableField(value = "barcode")
    private String barcode;

    /**
     * SKU状态：1-正常，2-缺货，3-下架
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 排序
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;
}
