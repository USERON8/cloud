package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类搜索事件
 * 用于分类数据变更时同步到搜索服务的Elasticsearch
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySearchEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件类型：CREATE, UPDATE, DELETE, STATUS_CHANGE
     */
    private String eventType;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类层级
     */
    private Integer level;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 分类图标
     */
    private String icon;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序值
     */
    private Integer sortOrder;

    /**
     * 商品数量
     */
    private Integer productCount;

    /**
     * 分类路径（用于层级搜索）
     */
    private String categoryPath;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;

    /**
     * 备注信息
     */
    private String remark;
}
