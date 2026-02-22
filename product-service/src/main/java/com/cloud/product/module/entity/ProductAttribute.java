package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;








@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_attribute")
@Data
public class ProductAttribute extends BaseEntity<ProductAttribute> {
    


    @TableField(value = "product_id")
    private Long productId;

    


    @TableField(value = "attr_name")
    private String attrName;

    


    @TableField(value = "attr_value")
    private String attrValue;

    


    @TableField(value = "attr_group")
    private String attrGroup;

    


    @TableField(value = "attr_type")
    private Integer attrType;

    


    @TableField(value = "is_filterable")
    private Integer isFilterable;

    


    @TableField(value = "is_list_visible")
    private Integer isListVisible;

    


    @TableField(value = "is_detail_visible")
    private Integer isDetailVisible;

    


    @TableField(value = "sort_order")
    private Integer sortOrder;

    


    @TableField(value = "unit")
    private String unit;
}
