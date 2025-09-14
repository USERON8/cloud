package com.cloud.common.domain.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类DTO
 * 用于服务间调用传输
 *
 * @author what's up
 * @since 1.0.0
 */
@Data
@Schema(description = "商品分类DTO")
public class CategoryDTO implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    private Long id;
    
    /**
     * 父分类ID
     */
    @Schema(description = "父分类ID")
    private Long parentId;
    
    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    private String name;
    
    /**
     * 分类描述
     */
    @Schema(description = "分类描述")
    private String description;
    
    /**
     * 分类图标URL
     */
    @Schema(description = "分类图标URL")
    private String iconUrl;
    
    /**
     * 分类图片URL
     */
    @Schema(description = "分类图片URL")
    private String imageUrl;
    
    /**
     * 排序权重
     */
    @Schema(description = "排序权重")
    private Integer sortOrder;
    
    /**
     * 分类级别
     */
    @Schema(description = "分类级别")
    private Integer level;
    
    /**
     * 分类路径（如：1,2,3）
     */
    @Schema(description = "分类路径")
    private String path;
    
    /**
     * 状态：0-禁用，1-启用
     */
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    
    /**
     * 是否显示：0-不显示，1-显示
     */
    @Schema(description = "是否显示：0-不显示，1-显示")
    private Integer isVisible;
    
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    
    /**
     * 逻辑删除标记
     */
    @Schema(description = "逻辑删除标记")
    private Boolean deleted;
    
    /**
     * 子分类列表
     */
    @Schema(description = "子分类列表")
    private java.util.List<CategoryDTO> children;
}
