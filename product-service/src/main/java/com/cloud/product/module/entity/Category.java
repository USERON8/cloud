package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商品分类表
 *
 * @TableName category
 */
@EqualsAndHashCode(callSuper = true)
@TableName("category")
@Data
public class Category extends BaseEntity<Category> {

    /**
     * 父分类ID，0表示根分类
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 分类名称
     */
    @TableField("name")
    private String name;

    /**
     * 层级，1表示一级分类，2表示二级分类，3表示三级分类
     */
    @TableField("level")
    private Integer level;

    /**
     * 排序字段
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态，1表示启用，0表示禁用
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建人ID
     */
    @TableField("create_by")
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField("update_by")
    private Long updateBy;

    /**
     * 子分类列表，该字段不映射到数据库
     */
    @TableField(exist = false)
    private List<Category> children;
}