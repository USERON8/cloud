package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sku")
public class Sku extends BaseEntity<Sku> {

    @TableField("spu_id")
    private Long spuId;

    @TableField("sku_code")
    private String skuCode;

    @TableField("sku_name")
    private String skuName;

    @TableField("spec_json")
    private String specJson;

    @TableField("sale_price")
    private BigDecimal salePrice;

    @TableField("market_price")
    private BigDecimal marketPrice;

    @TableField("cost_price")
    private BigDecimal costPrice;

    @TableField("status")
    private Integer status;

    @TableField("image_url")
    private String imageUrl;
}
