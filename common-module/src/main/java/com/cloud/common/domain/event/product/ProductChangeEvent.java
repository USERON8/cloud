package com.cloud.common.domain.event.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品变更事件对象
 * 
 * 标准字段设计：
 * - 核心标识: productId, shopId, eventType
 * - 关联字段: categoryId (分类关联)
 * - 状态变更: beforeStatus, afterStatus
 * - 追踪信息: timestamp, traceId
 * - 扩展信息: metadata, operator
 * 
 * @author CloudDevAgent
 * @since 2025-10-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductChangeEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 事件类型
     * CREATED - 创建商品
     * UPDATED - 更新商品
     * DELETED - 删除商品
     * STATUS_CHANGED - 状态变更
     * STOCK_CHANGED - 库存变更
     * PRICE_CHANGED - 价格变更
     */
    private String eventType;

    /**
     * 变更前状态
     * null表示新建操作
     */
    private Integer beforeStatus;

    /**
     * 变更后状态
     */
    private Integer afterStatus;

    /**
     * 事件发生时间
     */
    private LocalDateTime timestamp;

    /**
     * 分布式追踪ID (用于全链路追踪和幂等处理)
     */
    private String traceId;

    /**
     * 操作人标识
     */
    private String operator;

    /**
     * 扩展数据 (JSON格式)
     * 用于特定场景的额外信息，避免频繁修改事件结构
     * 
     * 示例:
     * - 创建商品: {"productName": "苹果", "price": "5.99", "stock": 100}
     * - 库存变更: {"beforeStock": 100, "changeStock": -10, "afterStock": 90}
     * - 价格变更: {"beforePrice": "5.99", "afterPrice": "4.99"}
     */
    private String metadata;
}
