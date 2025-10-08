package com.cloud.common.domain.event.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付变更事件对象
 * 
 * 标准字段设计：
 * - 核心标识: paymentId, orderId, userId, eventType
 * - 业务字段: amount (金额关联)
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
public class PaymentChangeEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 支付ID
     */
    private Long paymentId;

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
     * CREATED - 创建支付
     * UPDATED - 更新支付
     * STATUS_CHANGED - 状态变更
     * SUCCESS - 支付成功
     * FAILED - 支付失败
     * REFUND - 退款操作
     */
    private String eventType;

    /**
     * 支付金额 (核心业务字段)
     */
    private BigDecimal amount;

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
     * - 支付创建: {"paymentMethod": "ALIPAY", "transactionNo": "202501041234567"}
     * - 支付成功: {"paymentMethod": "WECHAT", "paidTime": "2025-01-04 10:30:00"}
     * - 退款: {"refundAmount": "50.00", "refundReason": "用户申请退款"}
     */
    private String metadata;
}
