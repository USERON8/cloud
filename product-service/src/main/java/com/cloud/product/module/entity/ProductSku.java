package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;








@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_sku")
@Data
public class ProductSku extends BaseEntity<ProductSku> {
    


    @TableField(value = "product_id")
    private Long productId;

    


    @TableField(value = "sku_code")
    private String skuCode;

    


    @TableField(value = "sku_name")
    private String skuName;

    


    @TableField(value = "spec_values")
    private String specValues;

    


    @TableField(value = "price")
    private BigDecimal price;

    


    @TableField(value = "original_price")
    private BigDecimal originalPrice;

    


    @TableField(value = "cost_price")
    private BigDecimal costPrice;

    


    @TableField(value = "stock_quantity")
    private Integer stockQuantity;

    


    @TableField(value = "sales_quantity")
    private Integer salesQuantity;

    


    @TableField(value = "image_url")
    private String imageUrl;

    


    @TableField(value = "weight")
    private Integer weight;

    


    @TableField(value = "volume")
    private Integer volume;

    


    @TableField(value = "barcode")
    private String barcode;

    


    @TableField(value = "status")
    private Integer status;

    


    @TableField(value = "sort_order")
    private Integer sortOrder;
}
