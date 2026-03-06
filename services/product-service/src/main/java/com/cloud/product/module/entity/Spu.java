package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("spu")
public class Spu extends BaseEntity<Spu> {

    @TableField("spu_name")
    private String spuName;

    @TableField("subtitle")
    private String subtitle;

    @TableField("category_id")
    private Long categoryId;

    @TableField("brand_id")
    private Long brandId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("status")
    private Integer status;

    @TableField("description")
    private String description;

    @TableField("main_image")
    private String mainImage;
}
