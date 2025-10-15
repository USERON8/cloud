package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品属性模板表
 * 预定义的属性集合,方便快速创建商品属性
 *
 * @author what's up
 * @TableName attribute_template
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "attribute_template")
@Data
public class AttributeTemplate extends BaseEntity<AttributeTemplate> {
    /**
     * 模板名称 (如: 手机模板、服装模板、食品模板等)
     */
    @TableField(value = "template_name")
    private String templateName;

    /**
     * 所属分类ID
     */
    @TableField(value = "category_id")
    private Long categoryId;

    /**
     * 属性列表 (JSON格式: [{"attr_name":"品牌","attr_type":1,"is_required":1},{"attr_name":"型号","attr_type":1,"is_required":1}])
     */
    @TableField(value = "attributes")
    private String attributes;

    /**
     * 模板描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 状态: 1-启用, 0-禁用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 是否系统预置: 0-否, 1-是
     */
    @TableField(value = "is_system")
    private Integer isSystem;

    /**
     * 使用次数
     */
    @TableField(value = "usage_count")
    private Integer usageCount;

    /**
     * 创建人ID
     */
    @TableField(value = "creator_id")
    private Long creatorId;
}
