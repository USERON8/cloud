package com.cloud.product.v2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sku")
public class SkuV2 extends BaseEntity<SkuV2> {

    @TableField("sku_no")
    private String skuNo;

    @TableField("spu_id")
    private Long spuId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("sku_code")
    private String skuCode;

    @TableField("sku_name")
    private String skuName;

    @TableField("specs_json")
    private String specsJson;

    @TableField("sale_price")
    private BigDecimal salePrice;

    @TableField("original_price")
    private BigDecimal originalPrice;

    @TableField("cost_price")
    private BigDecimal costPrice;

    @TableField("status")
    private String status;

    @TableField("sales_count")
    private Integer salesCount;

    @TableField("cover_image")
    private String coverImage;
}

