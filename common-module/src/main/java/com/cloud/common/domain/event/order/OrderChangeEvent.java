package com.cloud.common.domain.event.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单变更事件对象
 * 
 * 标准字段设计：
 * - 核心标识: orderId, userId, eventType
 * - 业务字段: totalAmount (金额关联)
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
public class OrderChangeEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 事件类型
     * CREATED - 创建订单
     * UPDATED - 更新订单
     * DELETED - 删除订单
     * STATUS_CHANGED - 状态变更
     * PAID - 支付完成
     * CANCELLED - 取消订单
     * COMPLETED - 订单完成
     */
    private String eventType;

    /**
     * 订单总额 (核心业务字段)
     */
    private BigDecimal totalAmount;

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
     * - 创建订单: {"payAmount": "99.00", "itemCount": 3}
     * - 支付完成: {"paymentId": 12345, "paymentMethod": "ALIPAY"}
     * - 取消订单: {"reason": "用户主动取消", "refundAmount": "99.00"}
     */
    private String metadata;
}
