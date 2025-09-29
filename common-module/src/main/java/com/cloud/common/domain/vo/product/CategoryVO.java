package com.cloud.common.domain.vo.product;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品分类VO
 */
@Data
public class CategoryVO {
    /**
     * 分类ID
     */
    private Long id;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 排序字段
     */
    private Integer sortOrder;

    /**
     * 状态，1表示启用，0表示禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 子分类列表
     */
    private List<CategoryVO> children;
}