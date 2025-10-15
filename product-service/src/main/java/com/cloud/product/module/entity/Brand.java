package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 品牌表
 *
 * @author what's up
 * @TableName brand
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "brand")
@Data
public class Brand extends BaseEntity<Brand> {
    /**
     * 品牌名称
     */
    @TableField(value = "brand_name")
    private String brandName;

    /**
     * 品牌英文名
     */
    @TableField(value = "brand_name_en")
    private String brandNameEn;

    /**
     * 品牌Logo URL
     */
    @TableField(value = "logo_url")
    private String logoUrl;

    /**
     * 品牌描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 品牌故事
     */
    @TableField(value = "brand_story")
    private String brandStory;

    /**
     * 品牌官网
     */
    @TableField(value = "official_website")
    private String officialWebsite;

    /**
     * 品牌国家/地区
     */
    @TableField(value = "country")
    private String country;

    /**
     * 成立年份
     */
    @TableField(value = "founded_year")
    private Integer foundedYear;

    /**
     * 品牌状态: 1-启用, 0-禁用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 是否热门: 0-否, 1-是
     */
    @TableField(value = "is_hot")
    private Integer isHot;

    /**
     * 是否推荐: 0-否, 1-是
     */
    @TableField(value = "is_recommended")
    private Integer isRecommended;

    /**
     * 关联商品数量
     */
    @TableField(value = "product_count")
    private Integer productCount;

    /**
     * 排序
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    /**
     * SEO关键词
     */
    @TableField(value = "seo_keywords")
    private String seoKeywords;

    /**
     * SEO描述
     */
    @TableField(value = "seo_description")
    private String seoDescription;
}
