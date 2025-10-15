package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SKU规格定义表
 * 定义商品可选的规格项(如颜色、尺寸等)
 *
 * @author what's up
 * @TableName sku_specification
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sku_specification")
@Data
public class SkuSpecification extends BaseEntity<SkuSpecification> {
    /**
     * 规格名称 (如: 颜色、尺寸、内存、容量等)
     */
    @TableField(value = "spec_name")
    private String specName;

    /**
     * 规格值列表 (JSON格式: ["红色","蓝色","黑色"])
     */
    @TableField(value = "spec_values")
    private String specValues;

    /**
     * 所属分类ID (0表示通用规格)
     */
    @TableField(value = "category_id")
    private Long categoryId;

    /**
     * 规格类型: 1-销售规格(影响价格库存), 2-展示规格(不影响价格库存)
     */
    @TableField(value = "spec_type")
    private Integer specType;

    /**
     * 是否必选: 0-否, 1-是
     */
    @TableField(value = "is_required")
    private Integer isRequired;

    /**
     * 排序
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    /**
     * 状态: 1-启用, 0-禁用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 规格描述
     */
    @TableField(value = "description")
    private String description;
}
