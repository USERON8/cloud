package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款创建事件
 * <p>
 * 当订单取消或需要退款时发送此事件，
 * 通知支付服务创建退款记录并进行退款处理
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundCreateEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 退款ID
     */
    private Long refundId;

    /**
     * 原支付记录ID
     */
    private Long originalPaymentId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 原支付金额
     */
    private BigDecimal originalAmount;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 退款类型
     * CANCEL - 订单取消退款
     * RETURN - 订单退货退款
     * PARTIAL - 部分退款
     */
    private String refundType;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 分布式追踪ID
     */
    private String traceId;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 备注
     */
    private String remark;
}
