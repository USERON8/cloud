package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;







@EqualsAndHashCode(callSuper = true)
@TableName(value = "products")
@Data
public class Product extends BaseEntity<Product> {
    


    @TableField(value = "product_name")
    private String name;

    


    @TableField(value = "description", exist = false)
    private String description;

    


    @TableField(value = "price")
    private BigDecimal price;

    


    @TableField(value = "category_id")
    private Long categoryId;

    


    @TableField(value = "category_name", exist = false)
    private String categoryName;

    



    @TableField(value = "brand_id", exist = false)
    private Long brandId;

    


    @TableField(value = "brand_name", exist = false)
    private String brandName;

    




    @Deprecated
    @TableField(value = "brand", exist = false)
    private String brand;

    


    @TableField(value = "sku", exist = false)
    private String sku;

    


    @TableField(value = "status")
    private Integer status;

    


    @TableField(value = "stock_quantity")
    private Integer stock;
    


    @TableField(value = "image_url", exist = false)
    private String imageUrl;
    


    @TableField(value = "detail_images", exist = false)
    private String detailImages;
    


    @TableField(value = "merchant_id", exist = false)
    private Long merchantId;
    


    @TableField(value = "merchant_name", exist = false)
    private String merchantName;
    


    @TableField(value = "shop_id")
    private Long shopId;
    


    @TableField(value = "shop_name", exist = false)
    private String shopName;
    


    @TableField(value = "tags", exist = false)
    private String tags;
    


    @TableField(value = "weight", exist = false)
    private BigDecimal weight;
    


    @TableField(value = "sales_count", exist = false)
    private Integer salesCount;
    


    @TableField(value = "sort_order", exist = false)
    private Integer sortOrder;
    


    @TableField(value = "is_recommended", exist = false)
    private Integer isRecommended;
    


    @TableField(value = "is_new", exist = false)
    private Integer isNew;
    


    @TableField(value = "is_hot", exist = false)
    private Integer isHot;
    


    @TableField(value = "remark", exist = false)
    private String remark;

    
    public Integer getStockQuantity() {
        return this.stock;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stock = stockQuantity;
    }
}

