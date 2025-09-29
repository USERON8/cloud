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
 * 支付成功事件
 * 用于支付成功后通知订单服务更新订单状态
 *
 * @author cloud
 * @date 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 支付ID
     */
    private Long paymentId;

    /**
     * 支付流水号
     */
    private String paymentNo;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 支付金额
     */
    private BigDecimal paymentAmount;

    /**
     * 实际支付金额
     */
    private BigDecimal actualAmount;

    /**
     * 支付方式：1-支付宝，2-微信，3-银行卡
     */
    private Integer paymentMethod;

    /**
     * 支付方式名称
     */
    private String paymentMethodName;

    /**
     * 第三方交易号
     */
    private String thirdPartyTransactionId;

    /**
     * 支付渠道
     */
    private String paymentChannel;

    /**
     * 支付状态：1-待支付，2-支付中，3-支付成功，4-支付失败，5-已退款
     */
    private Integer paymentStatus;

    /**
     * 支付前状态
     */
    private Integer beforeStatus;

    /**
     * 支付后状态
     */
    private Integer afterStatus;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 支付完成时间
     */
    private LocalDateTime completedTime;

    /**
     * 支付描述
     */
    private String description;

    /**
     * 支付备注
     */
    private String remark;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;

    /**
     * 回调数据（JSON格式）
     */
    private String callbackData;

    /**
     * 支付IP地址
     */
    private String paymentIp;

    /**
     * 设备信息
     */
    private String deviceInfo;
}
