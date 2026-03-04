package com.cloud.product.v2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spu")
public class SpuV2 extends BaseEntity<SpuV2> {

    @TableField("spu_no")
    private String spuNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("title")
    private String title;

    @TableField("sub_title")
    private String subTitle;

    @TableField("brand_name")
    private String brandName;

    @TableField("cover_image")
    private String coverImage;

    @TableField("detail_images")
    private String detailImages;

    @TableField("attributes_json")
    private String attributesJson;

    @TableField("sale_status")
    private String saleStatus;

    @TableField("audit_status")
    private String auditStatus;
}

