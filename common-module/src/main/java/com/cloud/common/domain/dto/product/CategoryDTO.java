package com.cloud.common.domain.dto.product;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品分类DTO
 *
 * @author 代码规范团队
 * @since 1.0.0
 */
@Data
public class CategoryDTO {
    
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
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 状态
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
     * 逻辑删除标识
     */
    private Integer deleted;
    
    /**
     * 子分类列表
     */
    private List<CategoryDTO> children;
}