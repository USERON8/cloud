package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;







@EqualsAndHashCode(callSuper = true)
@TableName(value = "brand")
@Data
public class Brand extends BaseEntity<Brand> {
    


    @TableField(value = "brand_name")
    private String brandName;

    


    @TableField(value = "brand_name_en")
    private String brandNameEn;

    


    @TableField(value = "logo_url")
    private String logoUrl;

    


    @TableField(value = "description")
    private String description;

    


    @TableField(value = "brand_story")
    private String brandStory;

    


    @TableField(value = "official_website")
    private String officialWebsite;

    


    @TableField(value = "country")
    private String country;

    


    @TableField(value = "founded_year")
    private Integer foundedYear;

    


    @TableField(value = "status")
    private Integer status;

    


    @TableField(value = "is_hot")
    private Integer isHot;

    


    @TableField(value = "is_recommended")
    private Integer isRecommended;

    


    @TableField(value = "product_count")
    private Integer productCount;

    


    @TableField(value = "sort_order")
    private Integer sortOrder;

    


    @TableField(value = "seo_keywords")
    private String seoKeywords;

    


    @TableField(value = "seo_description")
    private String seoDescription;
}
