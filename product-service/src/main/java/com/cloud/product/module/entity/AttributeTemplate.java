package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;








@EqualsAndHashCode(callSuper = true)
@TableName(value = "attribute_template")
@Data
public class AttributeTemplate extends BaseEntity<AttributeTemplate> {
    


    @TableField(value = "template_name")
    private String templateName;

    


    @TableField(value = "category_id")
    private Long categoryId;

    


    @TableField(value = "attributes")
    private String attributes;

    


    @TableField(value = "description")
    private String description;

    


    @TableField(value = "status")
    private Integer status;

    


    @TableField(value = "is_system")
    private Integer isSystem;

    


    @TableField(value = "usage_count")
    private Integer usageCount;

    


    @TableField(value = "creator_id")
    private Long creatorId;
}
