package com.cloud.product.v2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class CategoryV2 extends BaseEntity<CategoryV2> {

    @TableField("parent_id")
    private Long parentId;

    @TableField("category_name")
    private String categoryName;

    @TableField("level")
    private Integer level;

    @TableField("category_path")
    private String categoryPath;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private Integer status;
}

