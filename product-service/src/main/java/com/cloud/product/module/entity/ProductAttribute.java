package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品属性表
 * 用于描述商品的特性参数(如材质、产地、保质期等)
 *
 * @author what's up
 * @TableName product_attribute
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_attribute")
@Data
public class ProductAttribute extends BaseEntity<ProductAttribute> {
    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * 属性名称 (如: 材质、产地、保质期等)
     */
    @TableField(value = "attr_name")
    private String attrName;

    /**
     * 属性值
     */
    @TableField(value = "attr_value")
    private String attrValue;

    /**
     * 属性分组 (如: 基本信息、规格参数、包装信息等)
     */
    @TableField(value = "attr_group")
    private String attrGroup;

    /**
     * 属性类型: 1-文本, 2-数字, 3-日期, 4-图片, 5-富文本
     */
    @TableField(value = "attr_type")
    private Integer attrType;

    /**
     * 是否用于筛选: 0-否, 1-是
     */
    @TableField(value = "is_filterable")
    private Integer isFilterable;

    /**
     * 是否在列表页显示: 0-否, 1-是
     */
    @TableField(value = "is_list_visible")
    private Integer isListVisible;

    /**
     * 是否在详情页显示: 0-否, 1-是
     */
    @TableField(value = "is_detail_visible")
    private Integer isDetailVisible;

    /**
     * 排序
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    /**
     * 单位
     */
    @TableField(value = "unit")
    private String unit;
}
