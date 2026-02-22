package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;






@EqualsAndHashCode(callSuper = true)
@TableName("category")
@Data
public class Category extends BaseEntity<Category> {

    


    @TableField("parent_id")
    private Long parentId;

    


    @TableField("name")
    private String name;

    


    @TableField("level")
    private Integer level;

    


    @TableField("sort_order")
    private Integer sortOrder;

    


    @TableField("status")
    private Integer status;

    


    @TableField("create_by")
    private Long createBy;

    


    @TableField("update_by")
    private Long updateBy;

    


    @TableField(exist = false)
    private List<Category> children;
}
