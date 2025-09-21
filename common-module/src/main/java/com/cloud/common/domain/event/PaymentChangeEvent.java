package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付变更事件
 * 用于支付相关的事件传输
 *
 * @author cloud
 * @date 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentChangeEvent {

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 支付ID
     */
    private String paymentId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付前状态
     */
    private Integer beforeStatus;

    /**
     * 支付后状态
     */
    private Integer afterStatus;

    /**
     * 第三方交易号
     */
    private String thirdPartyTransactionId;

    /**
     * 支付描述
     */
    private String description;

    /**
     * 支付原因
     */
    private String reason;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 扩展信息
     */
    private Map<String, Object> metadata;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 事件时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 版本号
     */
    private Long version;
}
