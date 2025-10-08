package com.cloud.common.domain.event.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存变更事件对象
 * 
 * 标准字段设计：
 * - 核心标识: stockId, productId, eventType
 * - 关联字段: orderId (订单关联，可选)
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
public class StockChangeEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 库存ID
     */
    private Long stockId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 订单ID (可选，用于订单相关的库存变更)
     */
    private Long orderId;

    /**
     * 事件类型
     * CREATED - 创建库存
     * UPDATED - 更新库存
     * DELETED - 删除库存
     * RESERVED - 库存预占
     * CONFIRMED - 库存确认扣减
     * ROLLBACK - 库存回滚
     * QUANTITY_CHANGED - 数量变更
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
     * - 创建库存: {"productName": "苹果", "initialStock": 100}
     * - 库存预占: {"reserveQuantity": 10, "remainingStock": 90}
     * - 库存确认: {"confirmQuantity": 10, "beforeStock": 100, "afterStock": 90}
     * - 库存回滚: {"rollbackQuantity": 10, "reason": "订单取消"}
     */
    private String metadata;
}
