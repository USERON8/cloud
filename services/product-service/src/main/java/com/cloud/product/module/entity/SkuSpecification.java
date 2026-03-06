package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;








@EqualsAndHashCode(callSuper = true)
@TableName(value = "sku_specification")
@Data
public class SkuSpecification extends BaseEntity<SkuSpecification> {
    


    @TableField(value = "spec_name")
    private String specName;

    


    @TableField(value = "spec_values")
    private String specValues;

    


    @TableField(value = "category_id")
    private Long categoryId;

    


    @TableField(value = "spec_type")
    private Integer specType;

    


    @TableField(value = "is_required")
    private Integer isRequired;

    


    @TableField(value = "sort_order")
    private Integer sortOrder;

    


    @TableField(value = "status")
    private Integer status;

    


    @TableField(value = "description")
    private String description;
}
