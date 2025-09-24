package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品搜索事件
 * 用于商品数据变更时同步到搜索服务的Elasticsearch
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件类型：CREATE, UPDATE, DELETE, STATUS_CHANGE
     */
    private String eventType;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 库存数量
     */
    private Integer stockQuantity;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 商品状态：0-下架，1-上架
     */
    private Integer status;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品图片URL
     */
    private String imageUrl;

    /**
     * 商品标签（用于搜索）
     */
    private String tags;

    /**
     * 销量
     */
    private Integer salesCount;

    /**
     * 评分
     */
    private BigDecimal rating;

    /**
     * 评价数量
     */
    private Integer reviewCount;

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
