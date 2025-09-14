package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品表
 *
 * @author what's up
 * @TableName product
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "products")
@Data
public class Product extends BaseEntity<Product> {
    /**
     * 商品名称
     */
    @TableField(value = "product_name")
    private String name;

    /**
     * 商品描述（数据库扩展字段）
     */
    @TableField(value = "description", exist = false)
    private String description;

    /**
     * 商品价格
     */
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 商品分类 ID
     */
    @TableField(value = "category_id")
    private Long categoryId;

    /**
     * 商品分类名称（数据库扩展字段）
     */
    @TableField(value = "category_name", exist = false)
    private String categoryName;

    /**
     * 商品品牌ID
     */
    @TableField(value = "brand_id")
    private Long brandId;

    /**
     * 商品品牌名称（数据库扩展字段）
     */
    @TableField(value = "brand_name", exist = false)
    private String brandName;

    /**
     * 商品品牌（数据库扩展字段）
     *
     * @deprecated 使用brandName替代
     */
    @Deprecated
    @TableField(value = "brand", exist = false)
    private String brand;

    /**
     * SKU（数据库扩展字段）
     */
    @TableField(value = "sku", exist = false)
    private String sku;

    /**
     * 商品状态：0-下架，1-上架
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 库存数量
     */
    @TableField(value = "stock_quantity")
    private Integer stock;
    /**
     * 商品图片URL（数据库扩展字段）
     */
    @TableField(value = "image_url", exist = false)
    private String imageUrl;
    /**
     * 商品详情图片URLs（JSON格式）（数据库扩展字段）
     */
    @TableField(value = "detail_images", exist = false)
    private String detailImages;
    /**
     * 商家ID（数据库扩展字段）
     */
    @TableField(value = "merchant_id", exist = false)
    private Long merchantId;
    /**
     * 商家名称（数据库扩展字段）
     */
    @TableField(value = "merchant_name", exist = false)
    private String merchantName;
    /**
     * 店铺ID
     */
    @TableField(value = "shop_id")
    private Long shopId;
    /**
     * 店铺名称（数据库扩展字段）
     */
    @TableField(value = "shop_name", exist = false)
    private String shopName;
    /**
     * 商品标签（JSON格式）（数据库扩展字段）
     */
    @TableField(value = "tags", exist = false)
    private String tags;
    /**
     * 商品重量（克）（数据库扩展字段）
     */
    @TableField(value = "weight", exist = false)
    private BigDecimal weight;
    /**
     * 销量（数据库扩展字段）
     */
    @TableField(value = "sales_count", exist = false)
    private Integer salesCount;
    /**
     * 排序权重（数据库扩展字段）
     */
    @TableField(value = "sort_order", exist = false)
    private Integer sortOrder;
    /**
     * 是否推荐：0-不推荐，1-推荐（数据库扩展字段）
     */
    @TableField(value = "is_recommended", exist = false)
    private Integer isRecommended;
    /**
     * 是否新品：0-不是，1-是（数据库扩展字段）
     */
    @TableField(value = "is_new", exist = false)
    private Integer isNew;
    /**
     * 是否热销：0-不是，1-是（数据库扩展字段）
     */
    @TableField(value = "is_hot", exist = false)
    private Integer isHot;
    /**
     * 备注（数据库扩展字段）
     */
    @TableField(value = "remark", exist = false)
    private String remark;

    // 为了兼容性保留getter/setter别名
    public Integer getStockQuantity() {
        return this.stock;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stock = stockQuantity;
    }
}

